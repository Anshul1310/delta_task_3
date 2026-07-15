require('dotenv').config();

const express = require('express');
const cors = require('cors');
const http = require('http');
const WebSocket = require('ws');
const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');
const multer = require('multer');
const path = require('path');
const { v2: cloudinary } = require('cloudinary');
const { CloudinaryStorage } = require('multer-storage-cloudinary');
const { PrismaClient } = require('./generated/prisma');
const { Pool } = require('pg');
const { PrismaPg } = require('@prisma/adapter-pg');

const app = express();
const server = http.createServer(app);
const wss = new WebSocket.Server({ server: server });

const pool = new Pool({ connectionString: process.env.DATABASE_URL });
const adapter = new PrismaPg(pool);
const prisma = new PrismaClient({ adapter });

prisma.$connect()
    .then(() => {
        console.log('✅ Successfully connected to the database');
    })
    .catch((err) => {
        console.error('❌ Failed to connect to the database:', err);
    });

const JWT_SECRET = process.env.JWT_SECRET || 'dchat_super_secret_key_2024_anshul';
const PORT = 4000;

app.use(cors());
app.use(express.json());
app.use('/uploads', express.static(path.join(__dirname, 'uploads')));

cloudinary.config({
    cloud_name: process.env.CLOUDINARY_CLOUD_NAME,
    api_key: process.env.CLOUDINARY_API_KEY,
    api_secret: process.env.CLOUDINARY_API_SECRET
});

const storage = new CloudinaryStorage({
    cloudinary: cloudinary,
    params: {
        folder: 'dchat_uploads',
        resource_type: 'auto',
    },
});
const upload = multer({ storage: storage });


function authenticateToken(req, res, next) {
    var authHeader = req.headers['authorization'];
    if (!authHeader) {
        return res.status(401).json({ error: 'No token provided' });
    }

    var token = authHeader.split(' ')[1];
    if (!token) {
        return res.status(401).json({ error: 'No token provided' });
    }

    try {
        var decoded = jwt.verify(token, JWT_SECRET);
        req.userId = decoded.userId;
        next();
    } catch (error) {
        return res.status(403).json({ error: 'Invalid or expired token' });
    }
}


app.post('/signup', async function (req, res) {
    try {
        var name = req.body.name;
        var email = req.body.email;
        var password = req.body.password;

        if (!name || !email || !password) {
            return res.status(400).json({ error: 'Name, email, and password are required' });
        }

        var existingUser = await prisma.user.findUnique({
            where: { email: email }
        });
        if (existingUser) {
            return res.status(400).json({ error: 'Email is already registered' });
        }

        var hashedPassword = await bcrypt.hash(password, 10);

        var newUser = await prisma.user.create({
            data: {
                name: name,
                email: email,
                password: hashedPassword
            }
        });

        var token = jwt.sign({ userId: newUser.id }, JWT_SECRET, { expiresIn: '30d' });

        return res.status(201).json({
            message: 'Account created successfully',
            token: token,
            user: {
                id: newUser.id,
                name: newUser.name,
                email: newUser.email
            }
        });
    } catch (error) {
        console.error('Signup error:', error);
        return res.status(500).json({ error: 'Server error during signup' });
    }
});

app.post('/login', async function (req, res) {
    try {
        var email = req.body.email;
        var password = req.body.password;

        if (!email || !password) {
            return res.status(400).json({ error: 'Email and password are required' });
        }

        var user = await prisma.user.findUnique({
            where: { email: email }
        });
        if (!user) {
            return res.status(401).json({ error: 'Invalid email or password' });
        }

        var isPasswordValid = await bcrypt.compare(password, user.password);
        if (!isPasswordValid) {
            return res.status(401).json({ error: 'Invalid email or password' });
        }

        await prisma.user.update({
            where: { id: user.id },
            data: { isOnline: true, lastSeen: new Date() }
        });

        var token = jwt.sign({ userId: user.id }, JWT_SECRET, { expiresIn: '30d' });

        return res.status(200).json({
            message: 'Login successful',
            token: token,
            user: {
                id: user.id,
                name: user.name,
                email: user.email
            }
        });
    } catch (error) {
        console.error('Login error:', error);
        return res.status(500).json({ error: 'Server error during login' });
    }
});

app.get('/users', authenticateToken, async function (req, res) {
    try {
        var users = await prisma.user.findMany({
            where: {
                id: { not: req.userId }
            },
            select: {
                id: true,
                name: true,
                email: true,
                isOnline: true,
                lastSeen: true
            },
            orderBy: { name: 'asc' }
        });
        return res.status(200).json({ users: users });
    } catch (error) {
        console.error('Get users error:', error);
        return res.status(500).json({ error: 'Server error fetching users' });
    }
});

app.get('/messages/:peerId', authenticateToken, async function (req, res) {
    try {
        var peerId = req.params.peerId;
        var myId = req.userId;

        var messages = await prisma.message.findMany({
            where: {
                roomId: null,
                OR: [
                    { senderId: myId, receiverId: peerId },
                    { senderId: peerId, receiverId: myId }
                ]
            },
            include: {
                replyTo: {
                    select: {
                        id: true,
                        content: true,
                        senderId: true
                    }
                }
            },
            orderBy: { createdAt: 'asc' }
        });

        await prisma.message.updateMany({
            where: {
                senderId: peerId,
                receiverId: myId,
                status: { not: 'READ' }
            },
            data: { status: 'READ' }
        });

        return res.status(200).json({ messages: messages });
    } catch (error) {
        console.error('Get messages error:', error);
        return res.status(500).json({ error: 'Server error fetching messages' });
    }
});

app.get('/rooms', authenticateToken, async function (req, res) {
    try {
        var memberships = await prisma.roomMember.findMany({
            where: { userId: req.userId },
            include: {
                room: {
                    include: {
                        members: {
                            include: {
                                user: {
                                    select: { id: true, name: true, isOnline: true }
                                }
                            }
                        },
                        messages: {
                            orderBy: { createdAt: 'desc' },
                            take: 1
                        }
                    }
                }
            }
        });

        var rooms = memberships.map(function (membership) {
            return membership.room;
        });

        return res.status(200).json({ rooms: rooms });
    } catch (error) {
        console.error('Get rooms error:', error);
        return res.status(500).json({ error: 'Server error fetching rooms' });
    }
});

app.get("/health", (req, res) => {
    res.send("hello")
})

app.post('/rooms', authenticateToken, async function (req, res) {
    try {
        var name = req.body.name;
        var memberIds = req.body.memberIds;

        if (!name || !memberIds || memberIds.length === 0) {
            return res.status(400).json({ error: 'Group name and at least one member are required' });
        }

        if (memberIds.indexOf(req.userId) === -1) {
            memberIds.push(req.userId);
        }

        var membersData = memberIds.map(function (memberId) {
            return { userId: memberId };
        });

        var room = await prisma.room.create({
            data: {
                name: name,
                isGroup: true,
                members: {
                    create: membersData
                }
            },
            include: {
                members: {
                    include: {
                        user: {
                            select: { id: true, name: true, isOnline: true }
                        }
                    }
                }
            }
        });

        return res.status(201).json({ room: room });
    } catch (error) {
        console.error('Create room error:', error);
        return res.status(500).json({ error: 'Server error creating room' });
    }
});


app.get("/health", (req, res) => {
    res.status(200).json("hello")
})
app.get('/rooms/:roomId/messages', authenticateToken, async function (req, res) {
    try {
        var roomId = req.params.roomId;

        var messages = await prisma.message.findMany({
            where: { roomId: roomId },
            include: {
                sender: {
                    select: { id: true, name: true }
                },
                replyTo: {
                    select: {
                        id: true,
                        content: true,
                        senderId: true
                    }
                }
            },
            orderBy: { createdAt: 'asc' }
        });

        return res.status(200).json({ messages: messages });
    } catch (error) {
        console.error('Get room messages error:', error);
        return res.status(500).json({ error: 'Server error fetching room messages' });
    }
});

app.post('/rooms/:roomId/members', authenticateToken, async function (req, res) {
    try {
        var roomId = req.params.roomId;
        var memberIds = req.body.memberIds;

        if (!memberIds || memberIds.length === 0) {
            return res.status(400).json({ error: 'At least one member ID is required' });
        }

        var membersData = memberIds.map(function (memberId) {
            return { userId: memberId, roomId: roomId };
        });

        await prisma.roomMember.createMany({
            data: membersData,
            skipDuplicates: true
        });

        return res.status(200).json({ message: 'Members added successfully' });
    } catch (error) {
        console.error('Add members error:', error);
        return res.status(500).json({ error: 'Server error adding members' });
    }
});

app.post('/upload', authenticateToken, upload.single('file'), function (req, res) {
    try {
        if (!req.file) {
            return res.status(400).json({ error: 'No file uploaded' });
        }
        var fileUrl = req.file.path;
        return res.status(200).json({ url: fileUrl });
    } catch (error) {
        console.error('Upload error:', error);
        return res.status(500).json({ error: 'Server error uploading file' });
    }
});

app.post('/publickey', authenticateToken, async function (req, res) {
    try {
        var publicKey = req.body.publicKey;
        if (!publicKey) {
            return res.status(400).json({ error: 'Public key is required' });
        }

        await prisma.user.update({
            where: { id: req.userId },
            data: { publicKey: publicKey }
        });

        return res.status(200).json({ message: 'Public key saved' });
    } catch (error) {
        console.error('Save public key error:', error);
        return res.status(500).json({ error: 'Server error saving public key' });
    }
});

app.get('/publickey/:userId', authenticateToken, async function (req, res) {
    try {
        var user = await prisma.user.findUnique({
            where: { id: req.params.userId },
            select: { publicKey: true }
        });
        if (!user || !user.publicKey) {
            return res.status(404).json({ error: 'Public key not found' });
        }
        return res.status(200).json({ publicKey: user.publicKey });
    } catch (error) {
        console.error('Get public key error:', error);
        return res.status(500).json({ error: 'Server error fetching public key' });
    }
});


var activeClients = new Map();

wss.on('connection', function connection(ws) {

    ws.on('message', async function incoming(message) {
        try {
            var data = JSON.parse(message);

            if (data.type === 'register') {
                activeClients.set(data.senderId, ws);
                ws.userId = data.senderId;
                console.log('User ' + data.senderId + ' is online.');

                await prisma.user.update({
                    where: { id: data.senderId },
                    data: { isOnline: true, lastSeen: new Date() }
                });

                broadcastOnlineStatus(data.senderId, true);

                var undeliveredMessages = await prisma.message.findMany({
                    where: {
                        receiverId: data.senderId,
                        status: 'SENT'
                    }
                });
                for (var i = 0; i < undeliveredMessages.length; i++) {
                    var msg = undeliveredMessages[i];
                    await prisma.message.update({
                        where: { id: msg.id },
                        data: { status: 'DELIVERED' }
                    });

                    var senderSocket = activeClients.get(msg.senderId);
                    if (senderSocket && senderSocket.readyState === WebSocket.OPEN) {
                        senderSocket.send(JSON.stringify({
                            type: 'status_update',
                            messageId: msg.id,
                            status: 'DELIVERED'
                        }));
                    }
                }
            }

            else if (data.type === 'private') {
                var savedMessage = await prisma.message.create({
                    data: {
                        content: data.content,
                        senderId: data.senderId,
                        receiverId: data.targetId,
                        messageType: data.messageType || 'text',
                        mediaUrl: data.mediaUrl || null,
                        replyToId: data.replyToId || null,
                        status: 'SENT'
                    },
                    include: {
                        replyTo: {
                            select: {
                                id: true,
                                content: true,
                                senderId: true
                            }
                        }
                    }
                });

                ws.send(JSON.stringify({
                    type: 'message_sent',
                    id: savedMessage.id,
                    senderId: savedMessage.senderId,
                    receiverId: savedMessage.receiverId,
                    content: savedMessage.content,
                    messageType: savedMessage.messageType,
                    mediaUrl: savedMessage.mediaUrl,
                    status: 'SENT',
                    replyTo: savedMessage.replyTo,
                    createdAt: savedMessage.createdAt
                }));

                var targetSocket = activeClients.get(data.targetId);
                if (targetSocket && targetSocket.readyState === WebSocket.OPEN) {
                    targetSocket.send(JSON.stringify({
                        type: 'private',
                        id: savedMessage.id,
                        senderId: savedMessage.senderId,
                        content: savedMessage.content,
                        messageType: savedMessage.messageType,
                        mediaUrl: savedMessage.mediaUrl,
                        replyTo: savedMessage.replyTo,
                        createdAt: savedMessage.createdAt
                    }));

                    await prisma.message.update({
                        where: { id: savedMessage.id },
                        data: { status: 'DELIVERED' }
                    });

                    ws.send(JSON.stringify({
                        type: 'status_update',
                        messageId: savedMessage.id,
                        status: 'DELIVERED'
                    }));
                }
            }

            else if (data.type === 'group') {
                var savedGroupMessage = await prisma.message.create({
                    data: {
                        content: data.content,
                        senderId: data.senderId,
                        roomId: data.targetId,
                        messageType: data.messageType || 'text',
                        mediaUrl: data.mediaUrl || null,
                        replyToId: data.replyToId || null
                    },
                    include: {
                        sender: {
                            select: { id: true, name: true }
                        },
                        replyTo: {
                            select: {
                                id: true,
                                content: true,
                                senderId: true
                            }
                        }
                    }
                });

                ws.send(JSON.stringify({
                    type: 'group_message_sent',
                    id: savedGroupMessage.id,
                    targetId: savedGroupMessage.roomId,
                    senderId: savedGroupMessage.senderId,
                    senderName: savedGroupMessage.sender.name,
                    content: savedGroupMessage.content,
                    messageType: savedGroupMessage.messageType,
                    mediaUrl: savedGroupMessage.mediaUrl,
                    replyTo: savedGroupMessage.replyTo,
                    createdAt: savedGroupMessage.createdAt
                }));

                var members = await prisma.roomMember.findMany({
                    where: { roomId: data.targetId }
                });

                for (var j = 0; j < members.length; j++) {
                    var member = members[j];
                    if (member.userId !== data.senderId) {
                        var memberSocket = activeClients.get(member.userId);
                        if (memberSocket && memberSocket.readyState === WebSocket.OPEN) {
                            memberSocket.send(JSON.stringify({
                                type: 'group',
                                id: savedGroupMessage.id,
                                targetId: savedGroupMessage.roomId,
                                senderId: savedGroupMessage.senderId,
                                senderName: savedGroupMessage.sender.name,
                                content: savedGroupMessage.content,
                                messageType: savedGroupMessage.messageType,
                                mediaUrl: savedGroupMessage.mediaUrl,
                                replyTo: savedGroupMessage.replyTo,
                                createdAt: savedGroupMessage.createdAt
                            }));
                        }
                    }
                }
            }

            else if (data.type === 'typing') {
                if (data.targetType === 'private') {
                    var typingTarget = activeClients.get(data.targetId);
                    if (typingTarget && typingTarget.readyState === WebSocket.OPEN) {
                        typingTarget.send(JSON.stringify({
                            type: 'typing',
                            senderId: data.senderId,
                            isTyping: data.isTyping
                        }));
                    }
                } else if (data.targetType === 'group') {
                    var groupMembers = await prisma.roomMember.findMany({
                        where: { roomId: data.targetId }
                    });
                    for (var k = 0; k < groupMembers.length; k++) {
                        if (groupMembers[k].userId !== data.senderId) {
                            var gmSocket = activeClients.get(groupMembers[k].userId);
                            if (gmSocket && gmSocket.readyState === WebSocket.OPEN) {
                                gmSocket.send(JSON.stringify({
                                    type: 'typing',
                                    senderId: data.senderId,
                                    targetId: data.targetId,
                                    isTyping: data.isTyping
                                }));
                            }
                        }
                    }
                }
            }

            else if (data.type === 'read_receipt') {
                var messageIds = data.messageIds;
                if (messageIds && messageIds.length > 0) {
                    await prisma.message.updateMany({
                        where: {
                            id: { in: messageIds },
                            receiverId: data.senderId
                        },
                        data: { status: 'READ' }
                    });

                    var readMessages = await prisma.message.findMany({
                        where: { id: { in: messageIds } }
                    });

                    for (var m = 0; m < readMessages.length; m++) {
                        var originalSenderSocket = activeClients.get(readMessages[m].senderId);
                        if (originalSenderSocket && originalSenderSocket.readyState === WebSocket.OPEN) {
                            originalSenderSocket.send(JSON.stringify({
                                type: 'status_update',
                                messageId: readMessages[m].id,
                                status: 'READ'
                            }));
                        }
                    }
                }
            }

        } catch (error) {
            console.error('Error processing message:', error);
            ws.send(JSON.stringify({ error: 'Failed to process message' }));
        }
    });

    ws.on('close', async function () {
        var disconnectedUserId = ws.userId;
        if (disconnectedUserId) {
            activeClients.delete(disconnectedUserId);
            console.log('User ' + disconnectedUserId + ' went offline.');

            try {
                await prisma.user.update({
                    where: { id: disconnectedUserId },
                    data: { isOnline: false, lastSeen: new Date() }
                });
                broadcastOnlineStatus(disconnectedUserId, false);
            } catch (error) {
                console.error('Error updating offline status:', error);
            }
        }
    });
});

function broadcastOnlineStatus(userId, isOnline) {
    var statusMessage = JSON.stringify({
        type: 'online_status',
        userId: userId,
        isOnline: isOnline
    });

    activeClients.forEach(function (clientSocket, clientId) {
        if (clientId !== userId && clientSocket.readyState === WebSocket.OPEN) {
            clientSocket.send(statusMessage);
        }
    });
}

server.listen(PORT, '0.0.0.0', function () {
    console.log('DChat server is running on port ' + PORT);
    console.log('REST API: http://localhost:' + PORT);
    console.log('WebSocket: ws://localhost:' + PORT);
});
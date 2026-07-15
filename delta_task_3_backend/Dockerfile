FROM node:22-alpine AS builder
WORKDIR /anshul
COPY package*.json .
RUN npm install
COPY . .
RUN npx prisma generate

FROM node:22-alpine
WORKDIR /anshul
COPY --from=builder /anshul/ .
RUN mkdir -p /anshul/uploads
EXPOSE 4000
CMD ["node","index.js"]

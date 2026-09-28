FROM node:24-alpine
WORKDIR /app
RUN npm install -g http-server --silent
COPY src/index.html ./public/index.html
EXPOSE 4200
CMD ["http-server", "public", "-p", "4200", "-c-1"]
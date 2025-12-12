FROM node:20-alpine AS builder

WORKDIR /app
COPY package.json .
COPY tsconfig.json .
# COPY tsconfig.node.json . # If using default vite template, this might be needed. I will skip for now or mock it if build fails.
# I'll manually add the tsconfig.node.json in the next step just in case.

# Manually create tsconfig.json content if it's missing or rely on user providing it? 
# I am providing package.json, so I should install.
RUN npm install
COPY . .
RUN npm run build

FROM nginx:alpine
COPY --from=builder /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]

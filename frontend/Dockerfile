# ---- build the React app ----
FROM node:20-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
ARG VITE_GOOGLE_MAPS_API_KEY=""
ARG VITE_CITY_CENTER_LAT="12.9716"
ARG VITE_CITY_CENTER_LNG="77.5946"
ENV VITE_GOOGLE_MAPS_API_KEY=$VITE_GOOGLE_MAPS_API_KEY \
    VITE_CITY_CENTER_LAT=$VITE_CITY_CENTER_LAT \
    VITE_CITY_CENTER_LNG=$VITE_CITY_CENTER_LNG
RUN npm run build

# ---- serve it with nginx ----
FROM nginx:1.27-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80

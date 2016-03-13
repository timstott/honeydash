FROM nginx:1.9.12

# Remove existing configs
RUN rm /etc/nginx/conf.d/*.conf
COPY nginx.conf /etc/nginx/conf.d/

RUN mkdir -p /srv/www/honeydash/
COPY resources/public/ /srv/www/honeydash/
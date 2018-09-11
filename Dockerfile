FROM philipsahli/ubuntu-v2:trusty

RUN apt-get update 
#RUN apt-get upgrade -y

RUN ssh-keygen -A


RUN apt-get install -y postgresql-9.3 apg

# Workaround see: https://github.com/docker/docker/issues/783
RUN mkdir /etc/ssl/private-copy; mv /etc/ssl/private/* /etc/ssl/private-copy/; rm -r /etc/ssl/private; mv /etc/ssl/private-copy /etc/ssl/private; chmod -R 0700 /etc/ssl/private; chown -R postgres /etc/ssl/private

RUN echo "host all  all    0.0.0.0/0  md5" >> /etc/postgresql/9.3/main/pg_hba.conf
RUN echo "local   all             all                                     md5" >> /etc/postgresql/9.3/main/pg_hba.conf
RUN echo "listen_addresses='*'" >> /etc/postgresql/9.3/main/postgresql.conf


ADD startup_app.sh /
RUN chmod 755 /startup_app.sh && chown postgres /startup_app.sh

USER postgres


EXPOSE 5432

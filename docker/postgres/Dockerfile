FROM postgres:16.13-bookworm

ENV DEBIAN_FRONTEND=noninteractive \
	SCWS_VERSION=1.2.3 \
	SCWS_PREFIX=/usr/local/scws

RUN apt-get update \
	&& apt-get install -y --no-install-recommends \
		ca-certificates \
		build-essential \
		curl \
		git \
		postgresql-server-dev-16 \
	&& rm -rf /var/lib/apt/lists/*

RUN curl -fsSL "https://www.xunsearch.com/scws/down/scws-${SCWS_VERSION}.tar.bz2" -o /tmp/scws.tar.bz2 \
	&& mkdir -p /tmp/scws-src \
	&& tar -xjf /tmp/scws.tar.bz2 -C /tmp/scws-src --strip-components=1 \
	&& cd /tmp/scws-src \
	&& ./configure --prefix="${SCWS_PREFIX}" \
	&& make \
	&& make install \
	&& ldconfig \
	&& rm -rf /tmp/scws.tar.bz2 /tmp/scws-src

RUN git clone --depth=1 https://github.com/amutu/zhparser.git /tmp/zhparser \
	&& cd /tmp/zhparser \
	&& make SCWS_HOME="${SCWS_PREFIX}" \
	&& make install \
	&& rm -rf /tmp/zhparser

# Keep SCWS dictionary and rule paths explicit for easier troubleshooting.
ENV SCWS_XDICT=${SCWS_PREFIX}/etc/dict.utf8.xdb \
	SCWS_RULES=${SCWS_PREFIX}/etc/rules.utf8.ini

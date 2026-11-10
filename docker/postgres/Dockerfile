FROM postgres:16.13-bookworm@sha256:472efd9a66f2b2f1a5aeb18b28de74332e6ef88c2b93a1a5d812fb6db67a5f60

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

RUN git init /tmp/zhparser \
	&& cd /tmp/zhparser \
	&& git remote add origin https://github.com/amutu/zhparser.git \
	&& git fetch --depth=1 origin "2e995c4df672563992b4d7a147b8fa2d0d4cda6c" \
	&& git checkout --detach FETCH_HEAD \
	&& make SCWS_HOME="${SCWS_PREFIX}" \
	&& make install \
	&& rm -rf /tmp/zhparser

# Keep SCWS dictionary and rule paths explicit for easier troubleshooting.
ENV SCWS_XDICT=${SCWS_PREFIX}/etc/dict.utf8.xdb \
	SCWS_RULES=${SCWS_PREFIX}/etc/rules.utf8.ini

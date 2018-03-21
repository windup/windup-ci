FROM fedora:27

RUN dnf update -y \
    && groupadd --gid 995 jenkins \
    && groupadd --gid 1004 docker \
    && dnf install -y java-1.8.0-openjdk-devel-1.8.0.144 java-1.8.0-openjdk-headless-1.8.0.144 \
    && dnf install -y nano bzip2 unzip zip iproute wget python gcc-c++ \
    && dnf install -y docker \
    && dnf install -y nodejs git \
    && dnf install -y openssh openssh-server \
    && dnf --nogpgcheck install -y https://dl.google.com/linux/direct/google-chrome-stable_current_x86_64.rpm \
    && wget https://dl.yarnpkg.com/rpm/yarn.repo -O /etc/yum.repos.d/yarn.repo \
    && dnf install -y yarn \
    && npm install -g phantomjs-prebuilt --unsafe-perm \
    && npm install -g bower \
    && /usr/libexec/openssh/sshd-keygen rsa \
    && /usr/libexec/openssh/sshd-keygen ecdsa \
    && /usr/libexec/openssh/sshd-keygen ed25519 \
    && adduser jenkins --home-dir /var/lib/jenkins --gid 995 --uid 997 \
    && usermod -G docker -a jenkins \
    && mkdir /var/lib/jenkins/.docker \
    && mkdir /var/lib/jenkins/.ssh/ \
    && mkdir /var/lib/jenkins/.m2 \
    && mkdir /root/.ssh

COPY config.json /var/lib/jenkins/.docker
COPY settings.xml /var/lib/jenkins/.m2/settings.xml
COPY id_rsa.pub /var/lib/jenkins/.ssh/authorized_keys
COPY id_rsa.pub /root/.ssh/authorized_keys

RUN chown -R jenkins:jenkins /var/lib/jenkins/.docker \
    && chmod 0600 /var/lib/jenkins/.ssh/authorized_keys \
    && chown jenkins:jenkins /var/lib/jenkins/.ssh/authorized_keys \
    && chmod 0600 /root/.ssh/authorized_keys \
    && chown -R jenkins:jenkins /var/lib/jenkins/.m2

# Workaround for bug in ChromeDriver
# It cannot fully support headless mode yet, see https://github.com/SeleniumHQ/docker-selenium/issues/429
ENV DISPLAY :99
RUN dnf install -y Xvfb
RUN export DISPLAY=:99
COPY startup.sh /var/lib/jenkins/startup.sh
RUN chmod +x /var/lib/jenkins/startup.sh



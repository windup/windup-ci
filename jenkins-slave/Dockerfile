FROM fedora:25

RUN dnf update -y
RUN dnf install -y java-1.8.0-openjdk-devel java-1.8.0-openjdk-headless
RUN dnf install -y nano bzip2 unzip zip iproute wget
RUN dnf install -y docker
RUN dnf install -y nodejs git
RUN dnf install -y openssh openssh-server

RUN wget https://dl.yarnpkg.com/rpm/yarn.repo -O /etc/yum.repos.d/yarn.repo
RUN dnf install -y yarn

RUN npm install -g phantomjs-prebuilt
RUN npm install -g bower
RUN /usr/libexec/openssh/sshd-keygen rsa
RUN /usr/libexec/openssh/sshd-keygen ecdsa
RUN /usr/libexec/openssh/sshd-keygen ed25519
RUN groupadd --gid 995 jenkins
RUN groupadd --gid 1004 docker
RUN adduser jenkins --home-dir /var/lib/jenkins --gid 995 --uid 997
RUN usermod -G docker -a jenkins

RUN mkdir /var/lib/jenkins/.docker
COPY config.json /var/lib/jenkins/.docker
RUN chown -R jenkins:jenkins /var/lib/jenkins/.docker

RUN mkdir /var/lib/jenkins/.ssh/
COPY id_rsa.pub /var/lib/jenkins/.ssh/authorized_keys
RUN mkdir /root/.ssh
COPY id_rsa.pub /root/.ssh/authorized_keys
RUN chmod 0600 /var/lib/jenkins/.ssh/authorized_keys
RUN chown jenkins:jenkins /var/lib/jenkins/.ssh/authorized_keys
RUN chmod 0600 /root/.ssh/authorized_keys
RUN mkdir /var/lib/jenkins/.m2
COPY settings.xml /var/lib/jenkins/.m2/settings.xml
RUN chown -R jenkins:jenkins /var/lib/jenkins/.m2




FROM centos
COPY NERVE_Wallet.tar.gz ./
RUN tar -xvf ./NERVE_Wallet.tar.gz \
    && mv NERVE_Wallet /nerve \
    && rm -f NERVE_Wallet.tar.gz \
    && echo "tail -f /dev/null" >> /nerve/start

WORKDIR /nerve

CMD ["./start"]

RUN echo "successfully build nerve image"

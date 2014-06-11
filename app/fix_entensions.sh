#!/bin/sh

ln -s sha512-armv4.s openssl/crypto/sha/asm/sha512-armv4.S
ln -s sha256-armv4.s openssl/crypto/sha/asm/sha256-armv4.S
ln -s sha1-armv4-large.s openssl/crypto/sha/asm/sha1-armv4-large.S
ln -s armv4-mont.s openssl/crypto/bn/asm/armv4-mont.S
ln -s aes-armv4.s openssl/crypto/aes/asm/aes-armv4.S

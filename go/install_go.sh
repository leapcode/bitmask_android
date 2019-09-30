#!/bin/bash

GO_VERSION=go1.12.7.linux-amd64

curl -o $GO_VERSION.tar.gz https://dl.google.com/go/$GO_VERSION.tar.gz
tar -C ./golang -xzf $GO_VERSION.tar.gz

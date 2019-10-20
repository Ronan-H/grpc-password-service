Usage: java -jar grpc-password-service.jar [options]
  Options:
  	--port, -p
      Port to host the server on
      Default: 50051
    --hash-iterations, -hi
      Number of iterations to use for hashing
      Default: 10000
    --key-length, -kl
      Key length to use for hashing
      Default: 256
    --salt-length, -sl
      Length (number of bytes) of randomly generated salts
      Default: 32

Example:
  java -jar grpc-password-service.jar --port 8080 -hi 15000 -kl 512 -sl 64

- ALL arguments are optional, and can be specified in any order
- There is no validation on arguments aside from what JCommander does by default

Github URL: https://github.com/Ronan-H/grpc-password-service


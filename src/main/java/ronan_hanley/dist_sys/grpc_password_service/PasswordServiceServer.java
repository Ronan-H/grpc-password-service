package ronan_hanley.dist_sys.grpc_password_service;

import com.google.protobuf.ByteString;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import ronan_hanley.dist_sys.grpc_password_service.proto.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.logging.Logger;

public class PasswordServiceServer {
    private Server server;
    private static final Logger logger = Logger.getLogger(PasswordServiceServer.class.getName());

    private void start() throws IOException {
        /* The port on which the server should run */
        int port = 50051;
        server = ServerBuilder.forPort(port)
                .addService(new PasswordServiceImpl())
                .build()
                .start();
        logger.info("Server started, listening on " + port);
//        Runtime.getRuntime().addShutdownHook(new Thread() {
//            @Override
//            public void run() {
//                // Use stderr here since the logger may have been reset by its JVM shutdown hook.
//                System.err.println("*** shutting down gRPC server since JVM is shutting down");
//                PasswordServiceServer.this.stop();
//                System.err.println("*** server shut down");
//            }
//        });
    }

    private void stop() {
        if (server != null) {
            server.shutdown();
        }
    }

    /**
     * Await termination on the main thread since the grpc library uses daemon threads.
     */
    private void blockUntilShutdown() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
        }
    }

    static class TestPasswordServiceImpl extends PasswordServiceGrpc.PasswordServiceImplBase {
        @Override
        public void hash(HashRequest request, StreamObserver<HashResponse> responseObserver) {
            HashResponse hashResponse = HashResponse.newBuilder().setHashPair(
                    HashPair.newBuilder().setHash(
                            ByteString.copyFrom(new byte[] {1, 2, 3, 4, 5})
                    ).setSalt(
                            ByteString.copyFrom(new byte[] {15, 20, 25, 30})
                ).build()
            ).build();

            responseObserver.onNext(hashResponse);
            responseObserver.onCompleted();
        }

        @Override
        public void validate(ValidateRequest request, StreamObserver<ValidateResponse> responseObserver) {
            ValidateResponse validateResponse = ValidateResponse.newBuilder().setValid(true).build();

            responseObserver.onNext(validateResponse);
            responseObserver.onCompleted();
        }
    }

    static class PasswordServiceImpl extends PasswordServiceGrpc.PasswordServiceImplBase {
        @Override
        public void hash(HashRequest request, StreamObserver<HashResponse> responseObserver) {
            char[] pass = request.getPassword().toCharArray();
            byte[] salt = Passwords.getNextSalt();

            byte[] hash = Passwords.hash(pass, salt);

            HashResponse hashResponse = HashResponse.newBuilder()
                    .setUserId(request.getUserId())
                    .setHashPair(
                    HashPair.newBuilder().setHash(
                            ByteString.copyFrom(hash)
                    ).setSalt(
                            ByteString.copyFrom(salt)
                    ).build()
            ).build();

            responseObserver.onNext(hashResponse);
            responseObserver.onCompleted();
        }

        @Override
        public void validate(ValidateRequest request, StreamObserver<ValidateResponse> responseObserver) {
            char[] pass = request.getPassword().toCharArray();
            byte[] salt = request.getHashPair().getSalt().toByteArray();

            byte[] hash = Passwords.hash(pass, salt);
            byte[] requestHash = request.getHashPair().getHash().toByteArray();

            boolean isValid = Arrays.equals(hash, requestHash);

            ValidateResponse validateResponse = ValidateResponse.newBuilder().setValid(isValid).build();
            responseObserver.onNext(validateResponse);
            responseObserver.onCompleted();
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        final PasswordServiceServer server = new PasswordServiceServer();
        server.start();
        server.blockUntilShutdown();
    }
}
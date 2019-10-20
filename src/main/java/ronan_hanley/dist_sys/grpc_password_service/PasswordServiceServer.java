package ronan_hanley.dist_sys.grpc_password_service;

import com.beust.jcommander.JCommander;
import com.google.protobuf.ByteString;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import ronan_hanley.dist_sys.grpc_password_service.proto.*;

import java.io.IOException;
import java.util.logging.Logger;

public class PasswordServiceServer {
    private Server server;
    private static final Logger logger = Logger.getLogger(PasswordServiceServer.class.getName());

    private void start(CLIArgs jcArgs) throws IOException {
        // create Passwords object using JCommander arguments
        Passwords passwords = new Passwords(jcArgs.hashIterations, jcArgs.hashKeyLength, jcArgs.saltLength);

        // the port on which the server should run
        int port = jcArgs.port;
        server = ServerBuilder.forPort(port)
                .addService(new PasswordServiceImpl(passwords))
                .build()
                .start();
        logger.info("Server started, listening on " + port);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            // use stderr here since the logger may have been reset by its JVM shutdown hook.
            System.err.println("*** shutting down gRPC server since JVM is shutting down");
            PasswordServiceServer.this.stop();
            System.err.println("*** server shut down");
        }));
    }

    static class PasswordServiceImpl extends PasswordServiceGrpc.PasswordServiceImplBase {
        private Passwords passwords;

        public PasswordServiceImpl(Passwords passwords) {
            this.passwords = passwords;
        }

        @Override
        public void hash(HashRequest request, StreamObserver<HashResponse> responseObserver) {
            // retrieve password from request
            char[] pass = request.getPassword().toCharArray();
            // generate random salt
            byte[] salt = passwords.getNextSalt();

            // generate hash using password & salt
            byte[] hash = passwords.hash(pass, salt);

            // build response to send back to the client
            HashResponse hashResponse = HashResponse.newBuilder()
                .setUserId(request.getUserId())
                .setHashPair(
                    // build hash pair (stores hash & salt)
                    HashPair.newBuilder().setHash(
                        ByteString.copyFrom(hash)
                    ).setSalt(
                        ByteString.copyFrom(salt)
                    ).build()
            ).build();

            // send response
            responseObserver.onNext(hashResponse);
            responseObserver.onCompleted();
        }

        @Override
        public void validate(ValidateRequest request, StreamObserver<ValidateResponse> responseObserver) {
            // retrieve password, salt, and hash from request
            char[] pass = request.getPassword().toCharArray();
            byte[] salt = request.getHashPair().getSalt().toByteArray();
            byte[] requestHash = request.getHashPair().getHash().toByteArray();

            // perform validation
            boolean isValid = passwords.isExpectedPassword(pass, salt, requestHash);

            // build response
            ValidateResponse validateResponse = ValidateResponse.newBuilder().setValid(isValid).build();

            // send response
            responseObserver.onNext(validateResponse);
            responseObserver.onCompleted();
        }
    }

    private void stop() {
        if (server != null) {
            server.shutdown();
        }
    }

    /**
     * Await termination on the main thread since the grpc library uses daemon threads
     */
    private void blockUntilShutdown() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
        }
    }

    public static void main(String[] rawArgs) throws IOException, InterruptedException {
        // use JCommander to parse command line arguments
        CLIArgs jcArgs = new CLIArgs();

        JCommander jc = JCommander.newBuilder()
            .addObject(jcArgs)
            .build();

        jc.parse(rawArgs);
        jc.setProgramName("gRPC Passwords Service Server");

        // log command line arguments for debugging purposes
        StringBuilder toLog = new StringBuilder();
        toLog.append("Starting server with arguments...\n");
        toLog.append("\tPort = " + jcArgs.port).append("\n");
        toLog.append("\tHash iterations = " + jcArgs.hashIterations).append("\n");
        toLog.append("\tHash key length = " + jcArgs.hashKeyLength).append("\n");
        toLog.append("\tSalt length = " + jcArgs.saltLength).append("\n");
        logger.info(toLog.toString());

        final PasswordServiceServer server = new PasswordServiceServer();
        server.start(jcArgs);
        server.blockUntilShutdown();
    }
}
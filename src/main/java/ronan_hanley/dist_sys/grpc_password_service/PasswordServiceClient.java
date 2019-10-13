package ronan_hanley.dist_sys.grpc_password_service;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import ronan_hanley.dist_sys.grpc_password_service.proto.HashRequest;
import ronan_hanley.dist_sys.grpc_password_service.proto.HashResponse;
import ronan_hanley.dist_sys.grpc_password_service.proto.PasswordServiceGrpc;

import java.util.Base64;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PasswordServiceClient {
    private static final Logger logger = Logger.getLogger(PasswordServiceClient.class.getName());
    private final ManagedChannel channel;
    private final PasswordServiceGrpc.PasswordServiceBlockingStub clientStub;

    /** Construct client for accessing HelloWorld server using the existing channel. */
    public PasswordServiceClient(String host, int port) {
        this.channel = ManagedChannelBuilder.forAddress(host, port)
                // Channels are secure by default (via SSL/TLS). For the example we disable TLS to avoid
                // needing certificates.
                .usePlaintext()
                .build();
        clientStub = PasswordServiceGrpc.newBlockingStub(channel);
    }

    public void shutdown() throws InterruptedException {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }

    /** Say hello to server. */
    public void testHash(String pass) {
        logger.info("Will try to hash password...");
        HashRequest hashRequest = HashRequest.newBuilder().setUserId(0).setPassword(pass).build();
        HashResponse hashResponse;
        try {
            hashResponse = clientStub.hash(hashRequest);
        } catch (StatusRuntimeException e) {
            logger.log(Level.WARNING, "RPC failed: {0}", e.getStatus());
            return;
        }
        logger.info("Hash: " + Base64.getEncoder().encodeToString(hashResponse.getHashPair().getHash().toByteArray()));
        logger.info("Hash: " + Base64.getEncoder().encodeToString(hashResponse.getHashPair().getSalt().toByteArray()));
    }

    public static void main(String[] args) throws Exception {
        PasswordServiceClient client = new PasswordServiceClient("localhost", 50051);
        try {
            client.testHash("Test");
        } finally {
            client.shutdown();
        }
    }
}

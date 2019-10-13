package ronan_hanley.dist_sys.grpc_password_service;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import ronan_hanley.dist_sys.grpc_password_service.proto.*;

import java.util.Base64;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PasswordServiceClient {
    private static final Logger logger = Logger.getLogger(PasswordServiceClient.class.getName());
    private final ManagedChannel channel;
    private final PasswordServiceGrpc.PasswordServiceBlockingStub clientStub;

    /** Construct client for accessing password service server using the existing channel. */
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

    public void testHash(String pass) {
        logger.info(String.format("Testing hashing and validation for password \"%s\"...", pass));

        HashRequest hashRequest = HashRequest.newBuilder().setUserId(0).setPassword(pass).build();
        HashResponse hashResponse;

        try {
            hashResponse = clientStub.hash(hashRequest);
        } catch (StatusRuntimeException e) {
            logger.log(Level.WARNING, "RPC failed: {0}", e.getStatus());
            return;
        }

        ByteString hash = hashResponse.getHashPair().getHash();
        ByteString salt = hashResponse.getHashPair().getSalt();

        logger.info("Hash: " + Base64.getEncoder().encodeToString(hash.toByteArray()));
        logger.info("Salt: " + Base64.getEncoder().encodeToString(salt.toByteArray()));

        ValidateRequest validateRequest = ValidateRequest.newBuilder()
                .setPassword(pass)
                .setHashPair(HashPair.newBuilder()
                    .setHash(salt)
                    .setSalt(salt)
                ).build();

        ValidateResponse validateResponse;
        try {
            validateResponse = clientStub.validate(validateRequest);
        } catch (StatusRuntimeException e) {
            logger.log(Level.WARNING, "RPC failed: {0}", e.getStatus());
            return;
        }

        logger.info("Valid: " + validateResponse.getValid());
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

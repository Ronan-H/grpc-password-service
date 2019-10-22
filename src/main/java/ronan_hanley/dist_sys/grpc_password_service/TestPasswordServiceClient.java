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

/**
 * Client for testing the password service server.
 *
 * Purely for testing for part 1 for now, will be adjusted for use in part 2.
 */
public class TestPasswordServiceClient {
    private static final Logger logger = Logger.getLogger(TestPasswordServiceClient.class.getName());
    private final ManagedChannel channel;
    private final PasswordServiceGrpc.PasswordServiceBlockingStub clientStub;

    /** Construct client for accessing password service server using the existing channel. */
    public TestPasswordServiceClient(String host, int port) {
        this.channel = ManagedChannelBuilder.forAddress(host, port)
                // no TLS encryption
                .usePlaintext()
                .build();
        clientStub = PasswordServiceGrpc.newBlockingStub(channel);
    }

    public void testHash(String pass) {
        logger.info(String.format("Testing hashing and validation for password \"%s\"...", pass));

        // build hash request
        HashRequest hashRequest = HashRequest.newBuilder()
            .setUserId(0)
            .setPassword(pass)
        .build();

        HashResponse hashResponse;
        try {
            // get response
            hashResponse = clientStub.hash(hashRequest);
        } catch (StatusRuntimeException e) {
            logger.log(Level.WARNING, "RPC failed: {0}", e.getStatus());
            return;
        }

        // print hash and salt from response as Base64 text
        ByteString hash = hashResponse.getHashPair().getHash();
        ByteString salt = hashResponse.getHashPair().getSalt();
        logger.info("Hash: " + Base64.getEncoder().encodeToString(hash.toByteArray()));
        logger.info("Salt: " + Base64.getEncoder().encodeToString(salt.toByteArray()));

        // build validation request
        ValidateRequest validateRequest = ValidateRequest.newBuilder()
            .setPassword(pass)
            .setHashPair(HashPair.newBuilder()
                .setHash(hash)
                .setSalt(salt)
        ).build();

        ValidateResponse validateResponse;
        try {
            validateResponse = clientStub.validate(validateRequest);
        } catch (StatusRuntimeException e) {
            logger.log(Level.WARNING, "RPC failed: {0}", e.getStatus());
            return;
        }

        // print response (should be "true")
        logger.info("Valid: " + validateResponse.getValid());
    }

    public void shutdown() throws InterruptedException {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }

    public static void main(String[] args) throws Exception {
        TestPasswordServiceClient client = new TestPasswordServiceClient("localhost", 50051);
        try {
            client.testHash("password123");
        } finally {
            client.shutdown();
        }
    }
}

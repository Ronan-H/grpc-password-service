package ronan_hanley.dist_sys.grpc_password_service;

import com.beust.jcommander.Parameter;

public class CLIArgs {
    @Parameter(
            names = {"--usage", "-u"},
            description = "Print usage instructions and exit"
    )
    Boolean printUsage = false;

    @Parameter(
            names = {"--port", "-p"},
            description = "Port to host the server on"
    )
    Integer port = 50051;

    @Parameter(
            names = {"--hash-iterations", "-hi"},
            description = "Number of iterations to use for hashing"
    )
    Integer hashIterations = 10000;

    @Parameter(
            names = {"--key-length", "-kl"},
            description = "Key length to use for hashing"
    )
    Integer hashKeyLength = 256;

    @Parameter(
            names = {"--salt-length", "-sl"},
            description = "Length (number of bytes) of randomly generated salts"
    )
    Integer saltLength = 32;
}

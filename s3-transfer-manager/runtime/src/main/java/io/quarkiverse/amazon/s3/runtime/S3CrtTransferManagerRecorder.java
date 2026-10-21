package io.quarkiverse.amazon.s3.runtime;

import java.util.function.Function;

import io.quarkiverse.amazon.common.runtime.AsyncClientProxies;
import io.quarkus.arc.SyntheticCreationalContext;
import io.quarkus.runtime.annotations.Recorder;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.transfer.s3.S3TransferManager;

@Recorder
public class S3CrtTransferManagerRecorder {

    public Function<SyntheticCreationalContext<S3TransferManager>, S3TransferManager> getS3CrtTransferManager() {
        return new Function<SyntheticCreationalContext<S3TransferManager>, S3TransferManager>() {
            @Override
            public S3TransferManager apply(SyntheticCreationalContext<S3TransferManager> context) {
                S3AsyncClient client = context.getInjectedReference(S3AsyncClient.class, S3Crt.Literal.INSTANCE);
                return S3TransferManager.builder()
                        .s3Client(AsyncClientProxies.unwrap(client)).build();
            }
        };
    }
}

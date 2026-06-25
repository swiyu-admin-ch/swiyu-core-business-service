package ch.admin.bj.swiyu.core.business.test;

import ch.admin.bj.swiyu.core.business.common.utils.FileUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class VCTypeMetadataTestData {

    @Value("classpath:data/trust/vc-type-metadata-example-valid.json")
    private Resource validVcMetadata;

    @Value("classpath:data/trust/vc-type-metadata-example-invalid.json")
    private Resource invalidVcMetadata;

    @Value("classpath:data/trust/vc-type-metadata-example-invalid_wrong_vct.json")
    private Resource wrongVcMetadata;

    public String validTypeMetadata() {
        return FileUtil.asString(validVcMetadata);
    }

    public String invalidTypeMetadata() {
        return FileUtil.asString(invalidVcMetadata);
    }

    public String wrongVcMetadata() {
        return FileUtil.asString(wrongVcMetadata);
    }
}

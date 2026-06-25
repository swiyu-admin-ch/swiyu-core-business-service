package ch.admin.bj.swiyu.core.business.test;

import ch.admin.bj.swiyu.core.business.common.utils.FileUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StatuslistSchemaTestData {

    @Value("classpath:data/status/no-witness.json")
    private Resource noWitness;

    @Value("classpath:data/status/pub-key-multi.json")
    private Resource pubKeyMulti;

    @Value("classpath:data/status/controller.json")
    private Resource controller;

    public String noWitness() {
        return FileUtil.asString(noWitness);
    }

    public String pubKeyMulti() {
        return FileUtil.asString(pubKeyMulti);
    }

    public String controller() {
        return FileUtil.asString(controller);
    }
}

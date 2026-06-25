package ch.admin.bj.swiyu.core.business.common.did;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum DidMethod {
    DID_TDW("did:tdw"),
    DID_WEBVH("did:webvh");

    @Getter
    private final String didPrefix;
}

CREATE INDEX idx_vc_search_data_by_issuer ON vc_entity USING
    HASH ((vc_payload -> 'iss')) WHERE vc_type = 'sd-jwt';
CREATE INDEX idx_vc_search_data_by_issuer_and_time ON vc_entity USING
    BTREE ((vc_payload -> 'iss'), ((vc_payload -> 'exp')::bigint), ((vc_payload -> 'nbf')::bigint))
    WHERE vc_type = 'sd-jwt';

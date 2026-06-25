CREATE INDEX idx_vc_search_data_by_issuer ON vc_entity USING
    HASH ((encoded_vc -> 'iss')) WHERE vc_type = 'sd-jwt';
CREATE INDEX idx_vc_search_data_by_issuer_and_time ON vc_entity USING
    BTREE ((encoded_vc -> 'iss'), ((encoded_vc -> 'exp')::bigint), ((encoded_vc -> 'nbf')::bigint))
    WHERE vc_type = 'sd-jwt';

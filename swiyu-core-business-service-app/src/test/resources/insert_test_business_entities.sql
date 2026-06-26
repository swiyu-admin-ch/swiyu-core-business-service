INSERT INTO business_entity (id, entity_name, contact_email, created_by, created_at, last_modified_by, last_modified_at, type, is_verified)
values ('deadbeef-0000-0000-0000-000000000000', jsonb_build_object('default', 'Hello World AG'), 'hello.world@example.com', 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP, 'GOVERNMENTAL_INSTITUTION', true);
INSERT INTO business_entity (id, entity_name, contact_email, created_by, created_at, last_modified_by, last_modified_at, type, is_verified)
values ('deadbeef-deaf-0000-0000-000000000000', jsonb_build_object('default', 'FooBar GmbH'), 'foobar@example.com', 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP, 'UNKNOWN', false);
INSERT INTO business_entity (id, entity_name, contact_email, created_by, created_at, last_modified_by, last_modified_at, type, is_verified)
values ('deadbeef-deaf-beef-0000-000000000000', jsonb_build_object('default', 'Hello Second Entry AG'), 'foobar@example.com', 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP, 'UNKNOWN', false);


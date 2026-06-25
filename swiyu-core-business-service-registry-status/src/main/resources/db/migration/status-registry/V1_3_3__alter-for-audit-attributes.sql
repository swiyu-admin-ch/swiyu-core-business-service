-- Note: Alter statements are made for each column individually to be compatible with h2
-- Add audit attributes to the did_entity table
alter table vc_entity add column created_by varchar(255);
alter table vc_entity add column created_at timestamp;
alter table vc_entity add column last_modified_by varchar(255);
alter table vc_entity add column last_modified_at timestamp;

update vc_entity set
                      created_at = CURRENT_TIMESTAMP,
                      last_modified_at = CURRENT_TIMESTAMP,
                      created_by = 'system',
                      last_modified_by = 'system';

alter table vc_entity alter column created_at set not null;
alter table vc_entity alter column last_modified_at set not null;
alter table vc_entity alter column created_by set not null;
alter table vc_entity alter column last_modified_by set not null;

-- Add audit attributes to the datastore_entity table
alter table datastore_entity add column created_by varchar(255);
alter table datastore_entity add column created_at timestamp;
alter table datastore_entity add column last_modified_by varchar(255);
alter table datastore_entity add column last_modified_at timestamp;

update datastore_entity set
                            created_at = CURRENT_TIMESTAMP,
                            last_modified_at = CURRENT_TIMESTAMP,
                            created_by = 'system',
                            last_modified_by = 'system';

alter table datastore_entity alter column created_at set not null;
alter table datastore_entity alter column last_modified_at set not null;
alter table datastore_entity alter column created_by set not null;
alter table datastore_entity alter column last_modified_by set not null;
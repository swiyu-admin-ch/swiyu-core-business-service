-- Note: Alter statements are made for each column individually to be compatible with h2
-- Add audit attributes to the did_entity table
alter table business_entity add column created_by varchar(255);
alter table business_entity add column created_at timestamp;
alter table business_entity add column last_modified_by varchar(255);
alter table business_entity add column last_modified_at timestamp;

update business_entity set
                      created_at = CURRENT_TIMESTAMP,
                      last_modified_at = CURRENT_TIMESTAMP,
                      created_by = 'system',
                      last_modified_by = 'system';

alter table business_entity alter column created_at set not null;
alter table business_entity alter column last_modified_at set not null;
alter table business_entity alter column created_by set not null;
alter table business_entity alter column last_modified_by set not null;
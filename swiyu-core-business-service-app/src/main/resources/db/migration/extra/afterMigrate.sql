-- BIT specific script, see https://confluence.bit.admin.ch/x/z81rDw
-- Stored procedure used to reassign ownership of all objects owned by the current db user
-- to the dB group it belongs to (<database-name>_role_full)
create or replace procedure reassign_objects_ownership()
LANGUAGE 'plpgsql'
as $BODY$
BEGIN
 execute format('reassign owned by %s to %s_role_full', user, current_database());
END
$BODY$;

-- call this to actually reassign the ownership to the group
call reassign_objects_ownership();

-- delete the store procedure once done
drop procedure reassign_objects_ownership();

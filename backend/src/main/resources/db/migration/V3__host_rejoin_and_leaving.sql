-- V3: a host per group, personal rejoin codes, and members who can leave or be removed.

-- The host is the only member who can start the marathon or remove people.
ALTER TABLE members ADD COLUMN host boolean NOT NULL DEFAULT false;
-- For groups that already exist, the earliest member (the creator) becomes the host.
UPDATE members m SET host = true WHERE m.id = (SELECT min(id) FROM members WHERE group_id = m.group_id);
-- At most one host per group.
CREATE UNIQUE INDEX members_one_host_per_group ON members (group_id) WHERE host;

-- A short personal code that lets a member get back in from another browser or device.
ALTER TABLE members ADD COLUMN rejoin_code varchar(8);
UPDATE members SET rejoin_code = upper(substr(md5(random()::text || id::text), 1, 8));
ALTER TABLE members ALTER COLUMN rejoin_code SET NOT NULL;
CREATE UNIQUE INDEX members_group_rejoin_code_key ON members (group_id, rejoin_code);

-- A film stays in a marathon that has started even if its picker leaves, so a pick
-- must be able to outlive its member: the picker becomes NULL instead of the pick being deleted.
ALTER TABLE picks ALTER COLUMN member_id DROP NOT NULL;
ALTER TABLE picks DROP CONSTRAINT picks_member_id_fkey;
ALTER TABLE picks ADD CONSTRAINT picks_member_id_fkey
    FOREIGN KEY (member_id) REFERENCES members (id) ON DELETE SET NULL;

-- Members no longer rate their own pick, so remove any such ratings saved before this change.
DELETE FROM ratings r USING picks p WHERE r.pick_id = p.id AND r.member_id = p.member_id;

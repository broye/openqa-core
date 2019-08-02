/*
        OQA database sql.
        It is suggested to create the tables should be created in an independent database
        This is the mapping from domain to actual storage shard
        domain_shard : map domain to shard. Programmatically managed
        post : main data table for all posts, including question / answer / comment
        stats : question count by domain & topic
*/

create table domain_shard (
       domain varchar(60) PRIMARY KEY, -- domain name
       shard varchar(60), -- shard name, should be mapped to shard configured in config file
       description varchar(800), -- domain description
       last_update timestamp with time zone, -- last update
       create_date timestamp with time zone -- create date
);

create table post (
       aid UUID, --parent answer id, for question, null, for answer, pid, for comment, parent answerid
       answers_count integer default 0, -- answers count, applies to question only
       comments_count integer default 0, -- comments count, applies to questions and answers
       content text, -- post content
       content_external text , -- url for the content stored outside of postgresql. reserved. Prefer this to content if specified.
       content_lang char(1) , -- content markup language, h - html, m - markup
       create_date timestamp with time zone, -- create date
       domain varchar(80), -- domain name, will be mapped to shard, copied to entire thread
       downvote_count integer default 0, -- downvotes
       draft_content text, -- draft post content
       draft_content_external text , -- draft url for the content stored outside of postgresql. reserved. Prefer this to content if specified.
       draft_content_lang char(1) , -- draft content markup language, h - html, m - markup
       draft_title text, -- draft title
       folder varchar(80), -- folder, apply to question only
       last_active timestamp with time zone, -- last active, i.e. any new or update itself or subposts
       last_active_seq serial, -- serial number representing sequence of last update, larger newer.
       last_update timestamp with time zone, -- last update
       meta_tags varchar(80) [], -- meta tags, for non-functional use
       pid UUID primary key, -- post id
       qid UUID, -- parent question id, for question same to qid, for answer/comment: ancester question id
       reply_to_uid varchar(80), -- reply to user id
       reply_to_user_avatar text, -- reply to user's avatar
       reply_to_user_name text, -- reply to user's name
       seq_id serial, -- sequence id, unique and time sequential
       status char(1), -- publish status, p - published, d - deleted, i - initial draft, r - published and revising
       tags varchar(80) [], -- functional tags array
       title text, -- post title
       topic varchar(80), -- discussion topic, copied to entire thread
       type char(1), -- post type. q - question, a - answer, c -comment
       uid varchar(80), -- user id from external system
       upvote_count integer default 0, -- upvotes
       user_avatar text, -- user avatar url
       user_name text -- user name
);

create table stats (
       seq_id serial, -- sequence id
       domain varchar(80), -- domain
       topic varchar(80), -- topic
       folder varchar(80), -- folder
       question_count integer, -- questions count
       unique ( domain, topic, folder)
);

create table vote (
       seq_id serial, -- sequence id
       pid UUID, -- pid (question / answer / comment)
       uid varchar(80), -- external user id
       user_name text, -- external  user name
       user_avatar text, -- external user avatar
       type char(1), -- u : upvote, d : down vote
       primary key (pid, uid), -- user can vote only once
       last_update timestamp with time zone -- last update
);

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
       pid UUID primary key, -- post id
       seq_id serial, -- sequence id, unique and time sequential
       title text, -- post title
       content text, -- post content
       content_lang char(1) , -- content markup language, h - html, m - markup
       content_external text , -- url for the content stored outside of postgresql. reserved. Prefer this to content if specified.
       draft_title text, -- draft title
       draft_content text, -- draft post content
       draft_content_lang char(1) , -- draft content markup language, h - html, m - markup
       draft_content_external text , -- draft url for the content stored outside of postgresql. reserved. Prefer this to content if specified.
       domain varchar(80), -- domain name, will be mapped to shard, copied to entire thread
       topic varchar(80), -- discussion topic, copied to entire thread
       tags varchar(80) [], -- functional tags array
       meta_tags varchar(80) [], -- meta tags, for non-functional use
       type char(1), -- post type. q - question, a - answer, c -comment
       qid UUID, -- parent question id, for question same to qid, for answer/comment: ancester question id
       aid UUID, --parent answer id, for question, null, for answer, pid, for comment, parent answerid
       uid varchar(80), -- user id from external system
       user_name text, -- user name
       user_avatar text, -- user avatar url
       reply_to_uid varchar(80), -- reply to user id
       reply_to_user_name text, -- reply to user's name
       reply_to_user_avatar text, -- reply to user's avatar
       status char(1), -- publish status, p - published, d - deleted, i - initial draft, r - published and revising
       answers_count integer default 0, -- answers count, applies to question only
       comments_count integer default 0, -- comments count, applies to questions and answers
       upvote_count integer default 0, -- upvotes
       downvote_count integer default 0, -- downvotes
       folder varchar(80), -- folder, apply to question only
       create_date timestamp with time zone, -- create date
       last_update timestamp with time zone -- last update
);

create table stats (
       domain varchar(80), -- domain
       topic varchar(80), -- topic
       question_count integer, -- questions count
       primary key ( domain, topic)
);

create table vote (
       pid UUID, -- pid (question / answer / comment)
       uid varchar(80), -- external user id
       user_name text, -- external  user name
       user_avatar text, -- external user avatar
       type char(1), -- u : upvote, d : down vote
       primary key (pid, uid, type), -- user can vote only once
       last_update timestamp with time zone -- last update
);

/*
        OQA database sql.
        It is suggested to create the tables should be created in an independent database
        This is the mapping from domain to actual storage shard
        domain_shard : map domain to shard. Programmatically managed
        post : main data table for all posts, including question / answer / comment
*/

create table domain_shard (
       domain varchar(60) PRIMARY KEY, -- domain name
       shard varchar(60), -- shard name, should be mapped to shard configured in config file
       description varchar(800), -- domain description
       last_update timestamp with time zone, -- last update
       create_date timestamp with time zone -- create date
);

create table post (
       pid serial primary key, -- post id, auto increase serial
       title text, -- post title
       content text, -- post content
       content_lang char(1) , -- content markup language, h - html, m - markup
       content_external text , -- url for the content stored outside of postgresql. reserved. Prefer this to content if specified.
       domain varchar(80), -- domain name, will be mapped to shard, copied to entire thread
       topic varchar(80), -- discussion topic, copied to entire thread
       tags varchar(80) [], -- tags array
       type char(1), -- post type. q - question, a - answer, c -comment
       qid integer, -- parent question id, for question same to qid, for answer/comment: ancester question id
       aid integer, --parent answer id, for question, null, for answer, pid, for comment, parent answerid
       uid varchar(80), -- user id from external system
       user_name text, -- user name
       user_avatar text, -- user avatar url
       reply_to_uid varchar(80), -- reply to user id
       reply_to_user_name text, -- reply to user's name
       reply_to_user_avatar text, -- reply to user's avatar
       status char(1), -- post status, p - normal, e - editing, d - draft, t - deleted
       answers_count integer, -- answers count, applies to question only
       comments_count integer, -- comments count, applies to questions and answers
       upvote_count integer, -- upvotes
       downvote_count integer, -- downvotes
       create_date timestamp with time zone, -- create date
       last_update timestamp with time zone -- last update
);

create table votes (
       pid integer, -- pid (question / answer / comment)
       uid varchar(80), -- external user id
       uder_name text, -- external user name
       user_avatar text, -- external user avatar
       type char(1), -- u : upvote, d : down vote
       primary key (pid, uid, type), -- user can vote only once
       last_update timestamp with time zone -- last update
);
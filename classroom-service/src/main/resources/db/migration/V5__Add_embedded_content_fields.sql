-- Add embedded content fields to classroom_contents for storing lesson data directly
ALTER TABLE classroom_contents
ADD COLUMN title VARCHAR(255),
ADD COLUMN description VARCHAR(1000),
ADD COLUMN content TEXT;

-- Make contentId nullable (for lessons that don't reference external content)
ALTER TABLE classroom_contents
MODIFY COLUMN content_id BIGINT NULL;

-- Add comment
ALTER TABLE classroom_contents
COMMENT = 'Stores classroom content links and embedded lessons';

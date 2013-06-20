
drop table if exists caom2.Part;

create table caom2.Part
(
    name varchar(1024) not null,
    productType varchar(64),
    

-- internal
    obsID bigint not null,
    planeID bigint not null,
    metaRelease timestamp,
    artifactID bigint not null references caom2.Artifact (artifactID),
    partID bigint not null primary key using index tablespace caom_index,
    lastModified timestamp not null,
    maxLastModified timestamp not null,
    stateCode int not null
)
tablespace caom_data
;

create index i_artifactID on caom2.Part (artifactID)
tablespace caom_index
;

-- tag the clustering index
cluster i_artifactID on caom2.Part
;
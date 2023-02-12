-- create the table Parts
DROP TABLE IF EXISTS PART;
CREATE TABLE PART (
  part_number TEXT PRIMARY KEY,
  part_type TEXT NOT NULL CHECK (part_type IN ('Assembly', 'Manufactured', 'Provided'))
);

-- create the table Plans
DROP TABLE IF EXISTS PLAN;
CREATE TABLE PLAN (
  plan_id INTEGER PRIMARY KEY AUTOINCREMENT,
  part_number TEXT NOT NULL,
  leadtime INTEGER NOT NULL,
  component_part_number TEXT,
  component_part_quantity INTEGER NOT NULL,
  FOREIGN KEY (part_number) REFERENCES PART (part_number),
  FOREIGN KEY (component_part_number) REFERENCES PART (part_number)
);

-- create the table Operations
DROP TABLE IF EXISTS OPERATION;
CREATE TABLE OPERATION (
  operation_id INTEGER PRIMARY KEY AUTOINCREMENT,
  status TEXT NOT NULL CHECK (status IN ('Open', 'Done', 'Abort')),
  part_number TEXT NOT NULL,
  quantity INTEGER NOT NULL,
  start_date DATE NOT NULL,
  due_date DATE NOT NULL,
  planed_date DATE NOT NULL,
  charge INTEGER NOT NULL,
  workcenter TEXT,
  FOREIGN KEY (part_number) REFERENCES PART (part_number)
);


-- create the table Workcenter
DROP TABLE IF EXISTS WORKCENTER;
CREATE TABLE WORKCENTER (
  workcenter TEXT PRIMARY KEY,
  capacity INTEGER NOT NULL,
  setupTime INTEGER NOT NULL
);

-- create the table Demands
DROP TABLE IF EXISTS DEMAND;
CREATE TABLE DEMAND (
  demand_id INTEGER PRIMARY KEY AUTOINCREMENT,
  part_number TEXT NOT NULL,
  status TEXT NOT NULL CHECK (status IN ('Open', 'Fulfilled', 'Abort')),
  quantity INTEGER NOT NULL,
  due_date DATE NOT NULL,
  FOREIGN KEY (part_number) REFERENCES PART (part_number)
);

-- create the table Warehouse
DROP TABLE IF EXISTS WAREHOUSE;
CREATE TABLE WAREHOUSE (
  part_number TEXT NOT NULL PRIMARY KEY,
  quantity INTEGER NOT NULL,
  FOREIGN KEY (part_number) REFERENCES PART (part_number)
);

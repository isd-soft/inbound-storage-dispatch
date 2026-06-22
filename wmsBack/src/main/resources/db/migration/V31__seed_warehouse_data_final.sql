TRUNCATE TABLE
    transport_units, allocations, order_lines, orders,
    replenishments, tasks, inventory_history, stocks,
    locations, products, categories, users
    RESTART IDENTITY CASCADE;

INSERT INTO users (id, username, email, password, user_role, email_verified, is_active)
VALUES
    (1, 'dev', 'dev@isd.com', '$2a$12$Jgx.cGwjrw/ICdWSY4iYHuJ0eGKTRhfZ5IOO/tjrAtps/JkZ9J.vS', 'ROLE_DEV', true, true),
    (2, 'supervisor', 'super@isd.com', '$2a$12$Jgx.cGwjrw/ICdWSY4iYHuJ0eGKTRhfZ5IOO/tjrAtps/JkZ9J.vS', 'ROLE_SUPERVISOR', true, true),
    (3, 'operator', 'operator@isd.com', '$2a$12$Jgx.cGwjrw/ICdWSY4iYHuJ0eGKTRhfZ5IOO/tjrAtps/JkZ9J.vS', 'ROLE_OPERATOR', true, true),
    -- michael.scott -> password: Office%13
    (4, 'michael.scott', 'michael.scott@isd.com', '$2b$12$MfIbBa1ccvzcHJ7qkgxoxONeDzhSj3VN.N6GoyrukbYaFOZKomfcm', 'ROLE_SUPERVISOR', true, true),
    -- jim.halpert -> password: Dunder@38
    (5, 'jim.halpert', 'jim.halpert@isd.com', '$2b$12$lddj0PHxU3qWL2NV1KlCGuN/sLJ24YWQlm.On2USkocqlhZCwhY1i', 'ROLE_SUPERVISOR', true, true),
    -- pam.beesly -> password: Office!96
    (6, 'pam.beesly', 'pam.beesly@isd.com', '$2b$12$Sj0ztRyzCYVomhTmBWCVVu6HGwpoKq2Qr/XlM12gr50VD2.8UudiG', 'ROLE_OPERATOR', true, true),
    -- dwight.schrute -> password: WorldsBest#64
    (7, 'dwight.schrute', 'dwight.schrute@isd.com', '$2b$12$Xj4YCVOer/emSh67yuxFv.ThmWDK921ior6K0MASu1yTq9trMqUOq', 'ROLE_OPERATOR', true, true),
    -- stanley.hudson -> password: America$21
    (8, 'stanley.hudson', 'stanley.hudson@isd.com', '$2b$12$EUIHWo4v/atF1LdCHHXxf.c/G9nmvfNdJrrfK48Q89wPcmYC1oLIS', 'ROLE_SUPERVISOR', true, true),
    -- kevin.malone -> password: Dunder#74
    (9, 'kevin.malone', 'kevin.malone@isd.com', '$2b$12$I8ccluZbBfFleWzCyPXRoOytP0lE9pPp4r2UFOySfrqLk/Zlsheee', 'ROLE_OPERATOR', true, true),
    -- angela.martin -> password: BestBoss*35
    (10, 'angela.martin', 'angela.martin@isd.com', '$2b$12$Sq8uuwHMaqDzG776ixfKguxcKLex54QXOvHy5MGLM1XqYS2RYn1JW', 'ROLE_DEV', true, true),
    -- oscar.martinez -> password: Dunder%67
    (11, 'oscar.martinez', 'oscar.martinez@isd.com', '$2b$12$CifkinnNxJPiKLkmfgEuBe18bEMy4uq3f9HWrtN8sJli/JM5KTyu.', 'ROLE_OPERATOR', true, true),
    -- phyllis.vance -> password: America*30
    (12, 'phyllis.vance', 'phyllis.vance@isd.com', '$2b$12$xE4xBYFmEiFghX4tEx6st.fU5ENq8WabUt.HxCjSPvnKsB.ClE7QO', 'ROLE_OPERATOR', true, true),
    -- kelly.kapoor -> password: Scranton@45
    (13, 'kelly.kapoor', 'kelly.kapoor@isd.com', '$2b$12$.Nb0ZMYMdRXscObiPPUDKOllb/9zRCMYzdjE0fwbItEEVaWMPfM3u', 'ROLE_SUPERVISOR', true, true),
    -- toby.flenderson -> password: Dunder!53
    (14, 'toby.flenderson', 'toby.flenderson@isd.com', '$2b$12$/pQLchnod099waGEEpquau2uMjiurvr.TjFPnaeTQOVMDgj3EiQYG', 'ROLE_OPERATOR', true, true),
    -- creed.bratton -> password: Office!58
    (15, 'creed.bratton', 'creed.bratton@isd.com', '$2b$12$tHLmLzpS8FC1ClOSkWDtIOGKFwFVcO0QW1yT3tw9/BrjYhnzrJwYe', 'ROLE_OPERATOR', true, true),
    -- meredith.palmer -> password: Scranton%54
    (16, 'meredith.palmer', 'meredith.palmer@isd.com', '$2b$12$Jj/AFGZ0SHcAfQhy4BLzQuTFIFz8b99d59UMH13gMzIdM31dWxWom', 'ROLE_OPERATOR', true, true),
    -- ryan.howard -> password: America!68
    (17, 'ryan.howard', 'ryan.howard@isd.com', '$2b$12$fT0KoCncpB4shLEzCYonAuNExy9OJT5ONTUw/./jznRB1aDtsFlC2', 'ROLE_OPERATOR', true, true),
    -- darryl.philbin -> password: TheOffice%20
    (18, 'darryl.philbin', 'darryl.philbin@isd.com', '$2b$12$mu8koZ/uLu/sQNB6NRAau.Cemhi7dutDoRS62xYkpyCyNgMnunWC.', 'ROLE_OPERATOR', true, true),
    -- holly.flax -> password: WorldsBest$56
    (19, 'holly.flax', 'holly.flax@isd.com', '$2b$12$FC6M/w8YmCUnU08QCVUAi.c65r6.fO6UlZzthpjveV4D8P66e8.Ku', 'ROLE_DEV', true, true),
    -- jan.levinson -> password: Office$15
    (20, 'jan.levinson', 'jan.levinson@isd.com', '$2b$12$E7BhQ4/4ToACH1DTUpDH.OlAkWBQ95iBNQrajorvnOqqHF4875gQq', 'ROLE_SUPERVISOR', true, true),
    -- roy.anderson -> password: Mifflin$20
    (21, 'roy.anderson', 'roy.anderson@isd.com', '$2b$12$v0hJnwkT0yBMNekdxLZNreQsLzzdVRjFjfYafHBf8LOZ6O/55oQjO', 'ROLE_OPERATOR', true, true),
    -- david.wallace -> password: Office%58
    (22, 'david.wallace', 'david.wallace@isd.com', '$2b$12$fBRbZVlVwv.C5iwHlBdNTujpmQHFrtHTOOPsXtmCU7Gld.nLWHFMu', 'ROLE_SUPERVISOR', true, true),
    -- erin.hannon -> password: PaperSales&91
    (23, 'erin.hannon', 'erin.hannon@isd.com', '$2b$12$fCfBEChW3ZFigpDDXbI2quAPyeNwdyhx2lpm.r2mzkTvEMIlhsQ.6', 'ROLE_OPERATOR', true, true),
    -- gabe.lewis -> password: Warehouse&57
    (24, 'gabe.lewis', 'gabe.lewis@isd.com', '$2b$12$TiHtW28LCNvyf.xHeYI1XuRxWI.afb5x5Bcfgd7Ety7prjN2kwbtC', 'ROLE_OPERATOR', true, true),
    -- pete.miller -> password: Dunder%95
    (25, 'pete.miller', 'pete.miller@isd.com', '$2b$12$qSo/q6mRLei1gxsgZg0xgea1v6GjOeWS7eOGv6WBp7foW8rZM98Fa', 'ROLE_OPERATOR', true, true),
    -- clark.green -> password: Office@87
    (26, 'clark.green', 'clark.green@isd.com', '$2b$12$Z1mnPNqsGMPs2y1iinRGQubr.YimvsXnOPjWclKVQb2fBqHDvhxxq', 'ROLE_SUPERVISOR', true, true),
    -- nellie.bertram -> password: BestBoss@41
    (27, 'nellie.bertram', 'nellie.bertram@isd.com', '$2b$12$o1ZNTeINEmmy2C5TuTJjCO7qoSgamKiH/2SLk9M7hkO7CGoEGzLV6', 'ROLE_DEV', true, true),
    -- robert.california -> password: PaperSales%58
    (28, 'robert.california', 'robert.california@isd.com', '$2b$12$pVYRvUu2mHEDz7iUSkkH2eZTFwYYwbgzN5/fY46snv4RPrXuz3OyO', 'ROLE_OPERATOR', true, true),
    -- jo.bennett -> password: BestBoss&38
    (29, 'jo.bennett', 'jo.bennett@isd.com', '$2b$12$YQcW2P7nLTbPq0Tw3Cr0lODhJ4VOcikjE6iBTa3pJU541bmkHX4.K', 'ROLE_OPERATOR', true, true),
    -- charles.miner -> password: America#39
    (30, 'charles.miner', 'charles.miner@isd.com', '$2b$12$5uqLZF9svVh5C9DFBxCR3.fOZFmY4LGCTwcsSWx0jgHFL9jp5V1t2', 'ROLE_OPERATOR', true, true),
    -- karen.filippelli -> password: Scranton%61
    (31, 'karen.filippelli', 'karen.filippelli@isd.com', '$2b$12$VWadn1KReJiyzw8.NT4HV.MWibZ.6iuzhgVhMMqxg6BxCbWSfZmuu', 'ROLE_SUPERVISOR', true, true),
    -- josh.porter -> password: Office&37
    (32, 'josh.porter', 'josh.porter@isd.com', '$2b$12$4Z01eRtfbaNTZJIj/eovF.5aLsip9WSkBcW7CpLSTjax0zwybDMPK', 'ROLE_OPERATOR', true, true),
    -- ed.truck -> password: Dunder?93
    (33, 'ed.truck', 'ed.truck@isd.com', '$2b$12$zTK2DmWQJxMulNNF5WGBUe8DQhh.rOa4lM8mo3BZfaFoYJjCWQvXy', 'ROLE_OPERATOR', true, true),
    -- todd.packer -> password: TheOffice?92
    (34, 'todd.packer', 'todd.packer@isd.com', '$2b$12$M3vkLUOD/zH/J8BXo5HAn.7z5ojgyrRQpm1QWKMtn0ZvehUqbJxr2', 'ROLE_OPERATOR', true, true),
    -- craig.pelton -> password: Warehouse@43
    (35, 'craig.pelton', 'craig.pelton@isd.com', '$2b$12$iVdPX7SvHoz0/SODs6KvZevDDOiDODEgfcZYeWxI2sP4V9ViDKohS', 'ROLE_SUPERVISOR', true, true),
    -- jeff.winger -> password: Dunder%81
    (36, 'jeff.winger', 'jeff.winger@isd.com', '$2b$12$JKfJ2Ce.VwcrQLFTS.KLy.FH10VlkgCRi./sVbRiUx8W3bR6ipqyO', 'ROLE_OPERATOR', true, true),
    -- britta.perry -> password: WorldsBest*64
    (37, 'britta.perry', 'britta.perry@isd.com', '$2b$12$geH1jVsYQiK/wC47NByNoejMicHQ06BnBCy1yRU0PzJ/RLmVyWRbS', 'ROLE_OPERATOR', true, true),
    -- abed.nadir -> password: Scranton@38
    (38, 'abed.nadir', 'abed.nadir@isd.com', '$2b$12$cAwYWTYcXAvqGxQ8nv14xOOouW4yik.bFH7bGf4eNd27qxBS9v6cK', 'ROLE_OPERATOR', true, true),
    -- shirley.bennett -> password: BestBoss!73
    (39, 'shirley.bennett', 'shirley.bennett@isd.com', '$2b$12$MaYZ/Yboxxd8yXef3yfHMOpySCfs7Lm/Q7W7bpRbPZacawcnOWbmy', 'ROLE_SUPERVISOR', true, true),
    -- annie.edison -> password: America@24
    (40, 'annie.edison', 'annie.edison@isd.com', '$2b$12$LOKtTLwCgg5GI8c.1Frm9O1eUubaVuDyd.WvYQQph.8jnHvs78JBa', 'ROLE_OPERATOR', true, true),
    -- troy.barnes -> password: Warehouse*97
    (41, 'troy.barnes', 'troy.barnes@isd.com', '$2b$12$T7NWi2qwBiSAUFzhx78IzeLffViYg6lCVb/hXlvMERTEmXJsSjV06', 'ROLE_OPERATOR', true, true),
    -- pierce.hawthorne -> password: WorldsBest*18
    (42, 'pierce.hawthorne', 'pierce.hawthorne@isd.com', '$2b$12$VdAUYgcrWDLe2SJhDEkVCOr.Mhi4Jk2/gRtO2l6g5z4ffD6JO//oG', 'ROLE_OPERATOR', true, true),
    -- ben.chang -> password: TheOffice?86
    (43, 'ben.chang', 'ben.chang@isd.com', '$2b$12$QB2EMKicoJ6aAiU4EkN6RebUv422tMDyZhmkzfjFHzHnPwQLgDbOe', 'ROLE_OPERATOR', true, true),
    -- ian.duncan -> password: BestBoss#42
    (44, 'ian.duncan', 'ian.duncan@isd.com', '$2b$12$3/36ZtiOEEmr5FAL9N/HnuCGr9Lz.91TU227.RFtHVXgfboEe8kD2', 'ROLE_DEV', true, true),
    -- michelle.slater -> password: Office%97
    (45, 'michelle.slater', 'michelle.slater@isd.com', '$2b$12$XucH5HPG7d.7UwylMFnIXOfwn0gJVGAm9iF1g7/NEsAccxmpH7pk2', 'ROLE_OPERATOR', true, true),
    -- frankie.dart -> password: Scranton%24
    (46, 'frankie.dart', 'frankie.dart@isd.com', '$2b$12$aM3oJeP/nmh5d4PyEdcEte/M0q3zHxzYQvYUvBkvJNEgSTV5t20W.', 'ROLE_SUPERVISOR', true, true),
    -- elroy.patashnik -> password: TheOffice?30
    (47, 'elroy.patashnik', 'elroy.patashnik@isd.com', '$2b$12$5Kh5ATbmcdmh7S6YHOlnaOdn2IO5FXCIJQIfHMLVPnbgm6Qh3ClhG', 'ROLE_OPERATOR', true, true),
    -- buzz.hickey -> password: America@43
    (48, 'buzz.hickey', 'buzz.hickey@isd.com', '$2b$12$myd9Npdm55LLT2XndvSFteLfYLUUPPAaqFn/2lbc6bt4P6XsHEi52', 'ROLE_OPERATOR', true, true),
    -- leslie.knope -> password: BestBoss%23
    (49, 'leslie.knope', 'leslie.knope@isd.com', '$2b$12$L.dwCNZy5Ek/ZpdtahGMhOfNERv7uApUg/Tk4HNnz.2NhMurQeh4u', 'ROLE_OPERATOR', true, true),
    -- ron.swanson -> password: BestBoss$87
    (50, 'ron.swanson', 'ron.swanson@isd.com', '$2b$12$6oGsVqZe.RKf7sG.wCavfe8MMKn4Xlisz9OQtXJcHDfZbMvzWHA5O', 'ROLE_SUPERVISOR', true, true),
    -- tom.haverford -> password: Warehouse@57
    (51, 'tom.haverford', 'tom.haverford@isd.com', '$2b$12$yyBKotiY6gfrZmQU6sPX/OVBeCDXDasBwqgRMzJSAVuMcS2zULrT6', 'ROLE_DEV', true, true),
    -- april.ludgate -> password: BestBoss#77
    (52, 'april.ludgate', 'april.ludgate@isd.com', '$2b$12$bOGtCHIu43zQ7uUQpT6HA.av3YFrM09.ibwNKuVqOlt1TsWnP/asK', 'ROLE_OPERATOR', true, true),
    -- andy.dwyer -> password: WorldsBest?51
    (53, 'andy.dwyer', 'andy.dwyer@isd.com', '$2b$12$pK2T9WLN45QRi.bD6He3n.r7XYg7NZqj7bfXLj687egw0hCdTu8Ii', 'ROLE_OPERATOR', true, true);

INSERT INTO categories (id, name)
VALUES
    (1, 'Electronics'),
    (2, 'Food & Beverage'),
    (3, 'Household'),
    (4, 'Pharmacy'),
    (5, 'Warehouse Tools'),
    (6, 'Apparel & Uniforms'),
    (7, 'Automotive'),
    (8, 'Office Supplies'),
    (9, 'Chemicals & Safety'),
    (10, 'Packaging Materials');

INSERT INTO products (id, name, barcode, description, category_id, auto_replenish, min_threshold, replenish_qty)
VALUES
    (1, 'Laptop Pro 15', 'LAP-PRO-001', 'High-performance laptop', 1, false, NULL, NULL),
    (2, 'USB-C Cable 2m', 'CBL-USBC-2M', 'USB-C cable', 1, false, NULL, NULL),
    (3, 'HDMI Adapter', 'ADP-HDMI-01', '4K HDMI Adapter', 1, false, NULL, NULL),
    (4, 'Wireless Mouse', 'MSE-WLS-02', 'Ergonomic mouse', 1, true, 10, 50),
    (5, 'Mechanical Keyboard', 'KEY-MCH-05', 'RGB keyboard', 1, false, NULL, NULL),
    (6, 'A4 Paper Box', 'PAPER-A4-001', 'A4 paper 500 sheets', 8, true, 50, 200),
    (7, 'Bottled Water 0.5L', 'WTR-0500-24', 'Water 24 bottles', 2, true, 100, 300),
    (8, 'Canned Beans 400g', 'BEANS-400G', 'Canned beans', 2, false, NULL, NULL),
    (9, 'Energy Drink 0.33L', 'ENG-DRK-33', 'Energy drink pack 12', 2, true, 20, 100),
    (10, 'Instant Coffee 200g', 'COF-INS-200', 'Instant coffee jar', 2, true, 15, 60),
    (11, 'Green Tea Pack', 'TEA-GRN-50', 'Green tea 50 bags', 2, false, NULL, NULL),
    (12, 'Paper Towels 12R', 'TOWELS-12R', 'Paper towels 12 rolls', 3, true, 30, 100),
    (13, 'Trash Bags 60L', 'BAG-TRSH-60', 'Trash bags 50pcs', 3, true, 40, 120),
    (14, 'Liquid Hand Soap 5L', 'SOAP-LIQ-5L', 'Hand soap refill', 3, false, NULL, NULL),
    (15, 'Microfiber Cloths', 'CLTH-MCF-10', 'Cleaning cloths 10pcs', 3, true, 25, 80),
    (16, 'Dishwashing Liquid', 'DISH-LIQ-1L', 'Dish soap 1L', 3, false, NULL, NULL),
    (17, 'Hand Sanitizer 500ml', 'SAN-500ML', 'Hand sanitizer', 4, true, 20, 80),
    (18, 'Paracetamol 500mg', 'MED-PCM-500', 'Painkiller 20 tabs', 4, true, 50, 200),
    (19, 'Ibuprofen 400mg', 'MED-IBU-400', 'Anti-inflammatory 20 tabs', 4, true, 40, 150),
    (20, 'First Aid Kit Small', 'FAK-SM-01', 'First aid kit', 4, false, NULL, NULL),
    (21, 'Medical Face Masks', 'MSK-MED-50', 'Face masks box 50', 4, true, 30, 100),
    (22, 'Wireless Scanner', 'SCN-WLS-001', 'Barcode scanner', 5, true, 5, 20),
    (23, 'Thermal Label Roll', 'LBL-100X150', 'Thermal labels', 10, true, 10, 50),
    (24, 'Work Gloves L', 'GLOVE-W-01', 'Work gloves size L', 6, false, NULL, NULL),
    (25, 'Pallet Wrap 500m', 'WRAP-PAL-500', 'Stretch film 500m', 10, true, 5, 20),
    (26, 'Heavy Duty Utility Knife', 'KNIF-UTL-01', 'Box cutter', 5, true, 15, 50),
    (27, 'Replacement Blades 10x', 'BLD-RET-10', 'Spare blades pack', 5, true, 20, 100),
    (28, 'Packing Tape Clear', 'TAPE-CLR-6R', 'Packing tape 6 rolls', 10, true, 40, 150),
    (29, 'Tape Dispenser Gun', 'GUN-TAPE-01', 'Tape dispenser', 5, false, NULL, NULL),
    (30, 'Steel Toe Boots M', 'BOOT-STL-M', 'Safety boots medium', 6, false, NULL, NULL);

INSERT INTO locations (id, barcode, name, zone, description, available, is_active)
VALUES
    (1, 'PICK-A-01', 'PICK-A-01', 'PICKING', 'Pick Face A01', true, true),
    (2, 'PICK-A-02', 'PICK-A-02', 'PICKING', 'Pick Face A02', true, true),
    (3, 'PICK-A-03', 'PICK-A-03', 'PICKING', 'Pick Face A03', true, true),
    (4, 'PICK-A-04', 'PICK-A-04', 'PICKING', 'Pick Face A04', true, true),
    (5, 'PICK-A-05', 'PICK-A-05', 'PICKING', 'Pick Face A05', true, true),
    (6, 'PICK-B-01', 'PICK-B-01', 'PICKING', 'Pick Face B01', true, true),
    (7, 'PICK-B-02', 'PICK-B-02', 'PICKING', 'Pick Face B02', true, true),
    (8, 'PICK-B-03', 'PICK-B-03', 'PICKING', 'Pick Face B03', true, true),
    (9, 'PICK-B-04', 'PICK-B-04', 'PICKING', 'Pick Face B04', true, true),
    (10, 'PICK-B-05', 'PICK-B-05', 'PICKING', 'Pick Face B05', true, true),
    (11, 'PICK-C-01', 'PICK-C-01', 'PICKING', 'Pick Face C01', true, true),
    (12, 'PICK-C-02', 'PICK-C-02', 'PICKING', 'Pick Face C02', true, true),
    (13, 'PICK-C-03', 'PICK-C-03', 'PICKING', 'Pick Face C03', true, true),
    (14, 'PICK-C-04', 'PICK-C-04', 'PICKING', 'Pick Face C04', true, true),
    (15, 'PICK-C-05', 'PICK-C-05', 'PICKING', 'Pick Face C05', true, true),
    (16, 'REPL-A-01', 'REPL-A-01', 'REPLENISHMENT', 'Storage Rack A01', true, true),
    (17, 'REPL-A-02', 'REPL-A-02', 'REPLENISHMENT', 'Storage Rack A02', true, true),
    (18, 'REPL-A-03', 'REPL-A-03', 'REPLENISHMENT', 'Storage Rack A03', true, true),
    (19, 'REPL-A-04', 'REPL-A-04', 'REPLENISHMENT', 'Storage Rack A04', true, true),
    (20, 'REPL-A-05', 'REPL-A-05', 'REPLENISHMENT', 'Storage Rack A05', true, true),
    (21, 'REPL-B-01', 'REPL-B-01', 'REPLENISHMENT', 'Storage Rack B01', true, true),
    (22, 'REPL-B-02', 'REPL-B-02', 'REPLENISHMENT', 'Storage Rack B02', true, true),
    (23, 'REPL-B-03', 'REPL-B-03', 'REPLENISHMENT', 'Storage Rack B03', true, true),
    (24, 'REPL-B-04', 'REPL-B-04', 'REPLENISHMENT', 'Storage Rack B04', true, true),
    (25, 'REPL-B-05', 'REPL-B-05', 'REPLENISHMENT', 'Storage Rack B05', true, true),
    (26, 'REPL-C-01', 'REPL-C-01', 'REPLENISHMENT', 'Storage Rack C01', true, true),
    (27, 'REPL-C-02', 'REPL-C-02', 'REPLENISHMENT', 'Storage Rack C02', true, true),
    (28, 'REPL-C-03', 'REPL-C-03', 'REPLENISHMENT', 'Storage Rack C03', true, true),
    (29, 'REPL-C-04', 'REPL-C-04', 'REPLENISHMENT', 'Storage Rack C04', true, true),
    (30, 'REPL-C-05', 'REPL-C-05', 'REPLENISHMENT', 'Storage Rack C05', true, true),
    (31, 'DSP-01', 'DSP-01', 'DISPATCH', 'Dispatch Lane 01', true, true),
    (32, 'DSP-02', 'DSP-02', 'DISPATCH', 'Dispatch Lane 02', true, true),
    (33, 'DSP-03', 'DSP-03', 'DISPATCH', 'Dispatch Lane 03', true, true),
    (34, 'DSP-04', 'DSP-04', 'DISPATCH', 'Dispatch Lane 04', true, true),
    (35, 'DSP-05', 'DSP-05', 'DISPATCH', 'Dispatch Lane 05', true, true),
    (36, 'DSP-06', 'DSP-06', 'DISPATCH', 'Dispatch Lane 06', true, true),
    (37, 'DSP-07', 'DSP-07', 'DISPATCH', 'Dispatch Lane 07', true, true),
    (38, 'DSP-08', 'DSP-08', 'DISPATCH', 'Dispatch Lane 08', true, true),
    (39, 'DSP-09', 'DSP-09', 'DISPATCH', 'Dispatch Lane 09', true, true),
    (40, 'DSP-10', 'DSP-10', 'DISPATCH', 'Dispatch Lane 10', true, true),
    (41, 'DSP-11', 'DSP-11', 'DISPATCH', 'Dispatch Lane 11', true, true),
    (42, 'DSP-12', 'DSP-12', 'DISPATCH', 'Dispatch Lane 12', true, true),
    (43, 'DSP-13', 'DSP-13', 'DISPATCH', 'Dispatch Lane 13', true, true),
    (44, 'DSP-14', 'DSP-14', 'DISPATCH', 'Dispatch Lane 14', true, true),
    (45, 'DSP-15', 'DSP-15', 'DISPATCH', 'Dispatch Lane 15', true, true),
    (46, 'DSP-16', 'DSP-16', 'DISPATCH', 'Dispatch Lane 16', true, true),
    (47, 'DSP-17', 'DSP-17', 'DISPATCH', 'Dispatch Lane 17', true, true),
    (48, 'DSP-18', 'DSP-18', 'DISPATCH', 'Dispatch Lane 18', true, true),
    (49, 'DSP-19', 'DSP-19', 'DISPATCH', 'Dispatch Lane 19', true, true),
    (50, 'DSP-20', 'DSP-20', 'DISPATCH', 'Dispatch Lane 20', true, true);

INSERT INTO stocks (id, product_id, location_id, quantity, quantity_reserved, manufacture_date, expiration_date, version)
VALUES
    (1, 4, 1, 50, 0, '2025-12-01', '2099-12-31', 0),
    (2, 6, 2, 300, 0, '2025-12-05', '2099-12-31', 0),
    (3, 7, 3, 250, 0, '2025-12-10', '2027-12-10', 0),
    (4, 9, 4, 100, 0, '2025-12-15', '2027-12-15', 0),
    (5, 10, 5, 80, 0, '2025-12-20', '2027-12-20', 0),
    (6, 12, 6, 120, 0, '2026-01-05', '2099-12-31', 0),
    (7, 13, 7, 200, 0, '2026-01-10', '2099-12-31', 0),
    (8, 15, 8, 90, 0, '2026-01-15', '2099-12-31', 0),
    (9, 17, 9, 110, 0, '2026-01-20', '2028-01-20', 0),
    (10, 18, 10, 500, 0, '2026-01-25', '2029-01-25', 0),
    (11, 19, 11, 400, 0, '2026-02-01', '2029-02-01', 0),
    (12, 21, 12, 150, 0, '2026-02-05', '2099-12-31', 0),
    (13, 22, 13, 30, 0, '2026-02-10', '2099-12-31', 0),
    (14, 23, 14, 100, 0, '2026-02-15', '2099-12-31', 0),
    (15, 25, 15, 45, 0, '2026-02-20', '2099-12-31', 0),
    (16, 4, 16, 200, 0, '2025-12-01', '2099-12-31', 0),
    (17, 6, 17, 500, 0, '2025-12-05', '2099-12-31', 0),
    (18, 7, 18, 600, 0, '2025-12-10', '2027-12-10', 0),
    (19, 9, 19, 400, 0, '2025-12-15', '2027-12-15', 0),
    (20, 10, 20, 300, 0, '2025-12-20', '2027-12-20', 0),
    (21, 12, 21, 500, 0, '2026-01-05', '2099-12-31', 0),
    (22, 13, 22, 600, 0, '2026-01-10', '2099-12-31', 0),
    (23, 15, 23, 400, 0, '2026-01-15', '2099-12-31', 0),
    (24, 17, 24, 350, 0, '2026-01-20', '2028-01-20', 0),
    (25, 18, 25, 800, 0, '2026-01-25', '2029-01-25', 0),
    (26, 19, 26, 700, 0, '2026-02-01', '2029-02-01', 0),
    (27, 21, 27, 300, 0, '2026-02-05', '2099-12-31', 0),
    (28, 26, 28, 150, 0, '2026-02-10', '2099-12-31', 0),
    (29, 27, 29, 400, 0, '2026-02-15', '2099-12-31', 0),
    (30, 28, 30, 500, 0, '2026-02-20', '2099-12-31', 0);

INSERT INTO inventory_history (id, product_id, barcode, altered_quantity, previous_quantity, quantity_after_change, source_location_id, destination_location_id, operation_type, adjustment_reason, comment, timestamp, user_id)
VALUES
    (1, 4, 'MSE-WLS-02', 50, NULL, 50, NULL, 1, 'ADD_STOCK', NULL, 'Initial stock receipt', CURRENT_TIMESTAMP - INTERVAL '30 days', 4),
    (2, 6, 'PAPER-A4-001', 300, NULL, 300, NULL, 2, 'ADD_STOCK', NULL, 'Initial stock receipt', CURRENT_TIMESTAMP - INTERVAL '29 days', 5),
    (3, 7, 'WTR-0500-24', 250, NULL, 250, NULL, 3, 'ADD_STOCK', NULL, 'Initial stock receipt', CURRENT_TIMESTAMP - INTERVAL '28 days', 8),
    (4, 9, 'ENG-DRK-33', 100, NULL, 100, NULL, 4, 'ADD_STOCK', NULL, 'Initial stock receipt', CURRENT_TIMESTAMP - INTERVAL '27 days', 13),
    (5, 10, 'COF-INS-200', 80, NULL, 80, NULL, 5, 'ADD_STOCK', NULL, 'Initial stock receipt', CURRENT_TIMESTAMP - INTERVAL '26 days', 20),
    (6, 12, 'TOWELS-12R', 120, NULL, 120, NULL, 6, 'ADD_STOCK', NULL, 'Initial stock receipt', CURRENT_TIMESTAMP - INTERVAL '25 days', 22),
    (7, 13, 'BAG-TRSH-60', 200, NULL, 200, NULL, 7, 'ADD_STOCK', NULL, 'Initial stock receipt', CURRENT_TIMESTAMP - INTERVAL '24 days', 26),
    (8, 15, 'CLTH-MCF-10', 90, NULL, 90, NULL, 8, 'ADD_STOCK', NULL, 'Initial stock receipt', CURRENT_TIMESTAMP - INTERVAL '23 days', 31),
    (9, 17, 'SAN-500ML', 110, NULL, 110, NULL, 9, 'ADD_STOCK', NULL, 'Initial stock receipt', CURRENT_TIMESTAMP - INTERVAL '22 days', 35),
    (10, 18, 'MED-PCM-500', 500, NULL, 500, NULL, 10, 'ADD_STOCK', NULL, 'Initial stock receipt', CURRENT_TIMESTAMP - INTERVAL '21 days', 39),
    (11, 4, 'MSE-WLS-02', 200, NULL, 200, NULL, 16, 'ADD_STOCK', NULL, 'Bulk storage receipt', CURRENT_TIMESTAMP - INTERVAL '30 days', 46),
    (12, 6, 'PAPER-A4-001', 500, NULL, 500, NULL, 17, 'ADD_STOCK', NULL, 'Bulk storage receipt', CURRENT_TIMESTAMP - INTERVAL '29 days', 50),
    (13, 7, 'WTR-0500-24', 600, NULL, 600, NULL, 18, 'ADD_STOCK', NULL, 'Bulk storage receipt', CURRENT_TIMESTAMP - INTERVAL '28 days', 4),
    (14, 9, 'ENG-DRK-33', 400, NULL, 400, NULL, 19, 'ADD_STOCK', NULL, 'Bulk storage receipt', CURRENT_TIMESTAMP - INTERVAL '27 days', 5),
    (15, 10, 'COF-INS-200', 300, NULL, 300, NULL, 20, 'ADD_STOCK', NULL, 'Bulk storage receipt', CURRENT_TIMESTAMP - INTERVAL '26 days', 8),
    (16, 4, 'MSE-WLS-02', -5, 50, 45, 1, NULL, 'ADJUST_STOCK', 'DAMAGED', 'Damaged units removed', CURRENT_TIMESTAMP - INTERVAL '15 days', 4),
    (17, 12, 'TOWELS-12R', -3, 120, 117, 6, NULL, 'ADJUST_STOCK', 'LOST', 'Lost during count', CURRENT_TIMESTAMP - INTERVAL '10 days', 22),
    (18, 19, 'MED-IBU-400', -10, 400, 390, 11, NULL, 'ADJUST_STOCK', 'INVENTORY_MISMATCH', 'Mismatch found during audit', CURRENT_TIMESTAMP - INTERVAL '8 days', 39),
    (19, 7, 'WTR-0500-24', -30, 250, 220, 3, NULL, 'PICKING', NULL, 'Order completed', CURRENT_TIMESTAMP - INTERVAL '2 days', 7),
    (20, 13, 'BAG-TRSH-60', -40, 200, 160, 7, NULL, 'PICKING', NULL, 'Order completed', CURRENT_TIMESTAMP - INTERVAL '1 day', 11);

INSERT INTO tasks (id, supervisor_id, operator_id, task_type, requested_quantity, completed_at, status, created_at, updated_at)
VALUES
    (1,  4,  6,  'PICKING_ORDER', 10,  CURRENT_TIMESTAMP - INTERVAL '2 days',    'COMPLETED',   CURRENT_TIMESTAMP - INTERVAL '3 days',     CURRENT_TIMESTAMP - INTERVAL '2 days'),
    (2,  5,  7,  'PICKING_ORDER', 30,  CURRENT_TIMESTAMP - INTERVAL '2 days',    'COMPLETED',   CURRENT_TIMESTAMP - INTERVAL '3 days',     CURRENT_TIMESTAMP - INTERVAL '2 days'),
    (3,  8,  9,  'PICKING_ORDER', 50,  NULL,                                      'ASSIGNED',    CURRENT_TIMESTAMP - INTERVAL '2 hours',    CURRENT_TIMESTAMP - INTERVAL '30 minutes'),
    (4,  13, 11, 'PICKING_ORDER', 15,  CURRENT_TIMESTAMP - INTERVAL '2 days',    'COMPLETED',   CURRENT_TIMESTAMP - INTERVAL '3 days',     CURRENT_TIMESTAMP - INTERVAL '2 days'),
    (5,  20, 12, 'REPLENISHMENT', 100, NULL,                                      'ASSIGNED',    CURRENT_TIMESTAMP - INTERVAL '3 hours',    CURRENT_TIMESTAMP - INTERVAL '1 hour'),
    (6,  22, 14, 'PICKING_ORDER', 100, CURRENT_TIMESTAMP - INTERVAL '3 hours',   'COMPLETED',   CURRENT_TIMESTAMP - INTERVAL '5 hours',    CURRENT_TIMESTAMP - INTERVAL '3 hours'),
    (7,  26, 15, 'REPLENISHMENT', 80,  CURRENT_TIMESTAMP - INTERVAL '12 hours',  'COMPLETED',   CURRENT_TIMESTAMP - INTERVAL '1 day',      CURRENT_TIMESTAMP - INTERVAL '12 hours'),
    (8,  31, 16, 'PICKING_ORDER', 80,  NULL,                                      'ASSIGNED',    CURRENT_TIMESTAMP - INTERVAL '1 hour',     CURRENT_TIMESTAMP - INTERVAL '20 minutes'),
    (9,  35, 17, 'PICKING_ORDER', 20,  CURRENT_TIMESTAMP - INTERVAL '2 days',    'COMPLETED',   CURRENT_TIMESTAMP - INTERVAL '3 days',     CURRENT_TIMESTAMP - INTERVAL '2 days'),
    (10, 39, 18, 'REPLENISHMENT', 120, NULL,                                      'ASSIGNED',    CURRENT_TIMESTAMP - INTERVAL '2 hours',    CURRENT_TIMESTAMP - INTERVAL '45 minutes'),
    (11, 46, 21, 'PICKING_ORDER', 90,  CURRENT_TIMESTAMP - INTERVAL '3 hours',   'COMPLETED',   CURRENT_TIMESTAMP - INTERVAL '5 hours',    CURRENT_TIMESTAMP - INTERVAL '3 hours'),
    (12, 50, 23, 'PICKING_ORDER', 25,  NULL,                                      'ASSIGNED',    CURRENT_TIMESTAMP - INTERVAL '1 hour',     CURRENT_TIMESTAMP - INTERVAL '15 minutes'),
    (13, 4,  24, 'REPLENISHMENT', 90,  CURRENT_TIMESTAMP - INTERVAL '6 hours',   'COMPLETED',   CURRENT_TIMESTAMP - INTERVAL '12 hours',   CURRENT_TIMESTAMP - INTERVAL '6 hours'),
    (14, 5,  25, 'PICKING_ORDER', 60,  NULL,                                      'ASSIGNED',    CURRENT_TIMESTAMP - INTERVAL '1 hour',     CURRENT_TIMESTAMP - INTERVAL '10 minutes'),
    (15, 8,  28, 'PICKING_ORDER', 40,  CURRENT_TIMESTAMP - INTERVAL '1 day',     'COMPLETED',   CURRENT_TIMESTAMP - INTERVAL '2 days',     CURRENT_TIMESTAMP - INTERVAL '1 day'),
    (16, 13, 29, 'REPLENISHMENT', 50,  NULL,                                      'ASSIGNED',    CURRENT_TIMESTAMP - INTERVAL '3 hours',    CURRENT_TIMESTAMP - INTERVAL '1 hour'),
    (17, 20, 30, 'PICKING_ORDER', 55,  CURRENT_TIMESTAMP - INTERVAL '2 hours',   'COMPLETED',   CURRENT_TIMESTAMP - INTERVAL '4 hours',    CURRENT_TIMESTAMP - INTERVAL '2 hours'),
    (18, 22, 32, 'PICKING_ORDER', 12,  CURRENT_TIMESTAMP - INTERVAL '1 day',     'COMPLETED',   CURRENT_TIMESTAMP - INTERVAL '2 days',     CURRENT_TIMESTAMP - INTERVAL '1 day'),
    (19, 26, 33, 'PICKING_ORDER', 45,  NULL,                                      'ASSIGNED',    CURRENT_TIMESTAMP - INTERVAL '30 minutes', CURRENT_TIMESTAMP - INTERVAL '10 minutes'),
    (20, 31, 34, 'PICKING_ORDER', 35,  CURRENT_TIMESTAMP - INTERVAL '1 day',     'COMPLETED',   CURRENT_TIMESTAMP - INTERVAL '2 days',     CURRENT_TIMESTAMP - INTERVAL '1 day'),
    (21, 35, 36, 'PICKING_ORDER', 75,  CURRENT_TIMESTAMP - INTERVAL '2 hours',   'COMPLETED',   CURRENT_TIMESTAMP - INTERVAL '4 hours',    CURRENT_TIMESTAMP - INTERVAL '2 hours'),
    (22, 39, 37, 'PICKING_ORDER', 18,  CURRENT_TIMESTAMP - INTERVAL '1 day',     'COMPLETED',   CURRENT_TIMESTAMP - INTERVAL '2 days',     CURRENT_TIMESTAMP - INTERVAL '1 day'),
    (23, 46, 38, 'PICKING_ORDER', 70,  NULL,                                      'ASSIGNED',    CURRENT_TIMESTAMP - INTERVAL '2 hours',    CURRENT_TIMESTAMP - INTERVAL '30 minutes'),
    (24, 50, 40, 'PICKING_ORDER', 65,  CURRENT_TIMESTAMP - INTERVAL '1 hour',    'COMPLETED',   CURRENT_TIMESTAMP - INTERVAL '3 hours',    CURRENT_TIMESTAMP - INTERVAL '1 hour'),
    (25, 4,  41, 'PICKING_ORDER', 40,  NULL,                                      'CANCELED',    CURRENT_TIMESTAMP - INTERVAL '5 hours',    CURRENT_TIMESTAMP - INTERVAL '4 hours'),
    (26, 5,  42, 'REPLENISHMENT', 70,  NULL,                                      'CANCELED',    CURRENT_TIMESTAMP - INTERVAL '3 hours',    CURRENT_TIMESTAMP - INTERVAL '2 hours'),
    (27, 8,  43, 'PICKING_ORDER', 35,  NULL,                                      'CANCELED',    CURRENT_TIMESTAMP - INTERVAL '4 hours',    CURRENT_TIMESTAMP - INTERVAL '3 hours'),
    (28, 13, 45, 'PICKING_ORDER', 25,  NULL,                                      'CANCELED',    CURRENT_TIMESTAMP - INTERVAL '2 hours',    CURRENT_TIMESTAMP - INTERVAL '1 hour'),
    (29, 20, 47, 'REPLENISHMENT', 60,  NULL,                                      'CANCELED',    CURRENT_TIMESTAMP - INTERVAL '1 hour',     CURRENT_TIMESTAMP - INTERVAL '30 minutes'),
    (30, 22, 48, 'PICKING_ORDER', 50,  NULL,                                      'CANCELED',    CURRENT_TIMESTAMP - INTERVAL '6 hours',    CURRENT_TIMESTAMP - INTERVAL '5 hours'),
    (31, 26, 49, 'REPLENISHMENT', 110, NULL,                                      'ASSIGNED',    CURRENT_TIMESTAMP - INTERVAL '1 hour',     CURRENT_TIMESTAMP - INTERVAL '25 minutes'),
    (32, 31, 52, 'PICKING_ORDER', 28,  NULL,                                      'ASSIGNED',    CURRENT_TIMESTAMP - INTERVAL '45 minutes', CURRENT_TIMESTAMP - INTERVAL '15 minutes'),
    (33, 35, 53, 'REPLENISHMENT', 75,  NULL,                                      'ASSIGNED',    CURRENT_TIMESTAMP - INTERVAL '2 hours',    CURRENT_TIMESTAMP - INTERVAL '40 minutes');

INSERT INTO orders (id, logic_id, status, destination_location_id)
VALUES
    (1, 'ORD-2026-0001', 'COMPLETED', 31),
    (2, 'ORD-2026-0002', 'COMPLETED', 33),
    (3, 'ORD-2026-0003', 'IN_PROGRESS', 35),
    (4, 'ORD-2026-0004', 'SHORTAGE', 34),
    (5, 'ORD-2026-0005', 'COMPLETED', 32),
    (6, 'ORD-2026-0006', 'PARTIALLY_COMPLETED', 36),
    (7, 'ORD-2026-0007', 'IN_PROGRESS', 38),
    (8, 'ORD-2026-0008', 'COMPLETED', 37),
    (9, 'ORD-2026-0009', 'SHORTAGE', 39),
    (10, 'ORD-2026-0010', 'COMPLETED', 40),
    (11, 'ORD-2026-0011', 'IN_PROGRESS', 42),
    (12, 'ORD-2026-0012', 'PARTIALLY_COMPLETED', 41),
    (13, 'ORD-2026-0013', 'COMPLETED', 43),
    (14, 'ORD-2026-0014', 'IN_PROGRESS', 45),
    (15, 'ORD-2026-0015', 'COMPLETED', 44),
    (16, 'ORD-2026-0016', 'SHORTAGE', 46),
    (17, 'ORD-2026-0017', 'COMPLETED', 47),
    (18, 'ORD-2026-0018', 'IN_PROGRESS', 49),
    (19, 'ORD-2026-0019', 'PARTIALLY_COMPLETED', 48),
    (20, 'ORD-2026-0020', 'COMPLETED', 50),
    (21, 'ORD-2026-0021', 'CANCELED', 31),
    (22, 'ORD-2026-0022', 'CANCELED', 32),
    (23, 'ORD-2026-0023', 'CANCELED', 33),
    (24, 'ORD-2026-0024', 'CANCELED', 34),
    (25, 'ORD-2026-0025', 'CANCELED', 35),
    (26, 'ORD-2026-0026', 'IN_PROGRESS', 36),
    (27, 'ORD-2026-0027', 'IN_PROGRESS', 37);

INSERT INTO order_lines (id, order_id, task_id, product_id, requested_quantity, delivered_quantity, shortage_quantity, status)
VALUES
    (1, 1, 1, 4, 10, 10, 0, 'COMPLETED'),
    (2, 2, 2, 7, 30, 30, 0, 'COMPLETED'),
    (3, 3, 3, 19, 50, 0, 0, 'IN_PROGRESS'),
    (4, 4, 6, 26, 100, 0, 100, 'SHORTAGE'),
    (5, 5, 4, 9, 15, 15, 0, 'COMPLETED'),
    (6, 6, NULL, 21, 80, 50, 30, 'PARTIALLY_COMPLETED'),
    (7, 7, 8, 12, 80, 0, 0, 'IN_PROGRESS'),
    (8, 8, 9, 10, 20, 20, 0, 'COMPLETED'),
    (9, 9, 11, 27, 90, 0, 90, 'SHORTAGE'),
    (10, 10, 15, 13, 40, 40, 0, 'COMPLETED'),
    (11, 11, 12, 6, 25, 0, 0, 'IN_PROGRESS'),
    (12, 12, NULL, 22, 60, 25, 35, 'PARTIALLY_COMPLETED'),
    (13, 13, 18, 15, 12, 12, 0, 'COMPLETED'),
    (14, 14, 14, 17, 60, 0, 0, 'IN_PROGRESS'),
    (15, 15, 20, 18, 35, 35, 0, 'COMPLETED'),
    (16, 16, 17, 28, 55, 0, 55, 'SHORTAGE'),
    (17, 17, 22, 23, 18, 18, 0, 'COMPLETED'),
    (18, 18, 19, 25, 45, 0, 0, 'IN_PROGRESS'),
    (19, 19, NULL, 1, 70, 45, 25, 'PARTIALLY_COMPLETED'),
    (20, 20, 24, 19, 65, 65, 0, 'COMPLETED'),
    (21, 21, 25, 4, 40, 0, 0, 'CANCELED'),
    (22, 22, NULL, 6, 35, 0, 0, 'CANCELED'),
    (23, 23, NULL, 7, 50, 0, 0, 'CANCELED'),
    (24, 24, 28, 9, 25, 0, 0, 'CANCELED'),
    (25, 25, NULL, 10, 30, 0, 0, 'CANCELED'),
    (26, 26, 23, 21, 70, 0, 0, 'IN_PROGRESS'),
    (27, 27, 32, 12, 28, 0, 0, 'IN_PROGRESS');

INSERT INTO replenishments (id, task_id, product_id, requested_quantity, status, destination_location_id)
VALUES
    (1, 7, 7, 80, 'COMPLETED', 3),
    (2, 13, 10, 90, 'COMPLETED', 5),
    (3, 5, 6, 100, 'IN_PROGRESS', 2),
    (4, 10, 9, 120, 'IN_PROGRESS', 4),
    (5, 16, 12, 50, 'IN_PROGRESS', 6),
    (6, 31, 13, 110, 'IN_PROGRESS', 7),
    (7, 33, 17, 75, 'IN_PROGRESS', 9),
    (8, 26, 15, 70, 'CANCELED', 8),
    (9, 29, 18, 60, 'CANCELED', 10),
    (10, NULL, 19, 85, 'CANCELED', 11);

INSERT INTO allocations (id, task_id, quantity, stock_id, status, source_location_scanned, product_scanned, picked_quantity, created_at, updated_at)
VALUES
    (1,  1,  10,  1,  'COMPLETED',   true,  true,  10, CURRENT_TIMESTAMP - INTERVAL '3 days',     CURRENT_TIMESTAMP - INTERVAL '2 days'),
    (2,  2,  30,  3,  'COMPLETED',   true,  true,  30, CURRENT_TIMESTAMP - INTERVAL '3 days',     CURRENT_TIMESTAMP - INTERVAL '2 days'),
    (3,  3,  50,  11, 'IN_PROGRESS', false, false, 0,  CURRENT_TIMESTAMP - INTERVAL '2 hours',    CURRENT_TIMESTAMP - INTERVAL '30 minutes'),
    (4,  4,  15,  4,  'COMPLETED',   true,  true,  15, CURRENT_TIMESTAMP - INTERVAL '3 days',     CURRENT_TIMESTAMP - INTERVAL '2 days'),
    (5,  5,  100, 17, 'IN_PROGRESS', false, false, 0,  CURRENT_TIMESTAMP - INTERVAL '3 hours',    CURRENT_TIMESTAMP - INTERVAL '1 hour'),
    (6,  6,  100, 28, 'COMPLETED',   true,  true,  0,  CURRENT_TIMESTAMP - INTERVAL '5 hours',    CURRENT_TIMESTAMP - INTERVAL '3 hours'),
    (7,  7,  80,  18, 'COMPLETED',   true,  true,  80, CURRENT_TIMESTAMP - INTERVAL '1 day',      CURRENT_TIMESTAMP - INTERVAL '12 hours'),
    (8,  8,  80,  12, 'IN_PROGRESS', false, false, 0,  CURRENT_TIMESTAMP - INTERVAL '1 hour',     CURRENT_TIMESTAMP - INTERVAL '20 minutes'),
    (9,  9,  20,  5,  'COMPLETED',   true,  true,  20, CURRENT_TIMESTAMP - INTERVAL '3 days',     CURRENT_TIMESTAMP - INTERVAL '2 days'),
    (10, 10, 120, 19, 'IN_PROGRESS', false, false, 0,  CURRENT_TIMESTAMP - INTERVAL '2 hours',    CURRENT_TIMESTAMP - INTERVAL '45 minutes'),
    (11, 11, 90,  29, 'COMPLETED',   true,  true,  0,  CURRENT_TIMESTAMP - INTERVAL '5 hours',    CURRENT_TIMESTAMP - INTERVAL '3 hours'),
    (12, 12, 25,  2,  'IN_PROGRESS', false, false, 0,  CURRENT_TIMESTAMP - INTERVAL '1 hour',     CURRENT_TIMESTAMP - INTERVAL '15 minutes'),
    (13, 13, 90,  20, 'COMPLETED',   true,  true,  90, CURRENT_TIMESTAMP - INTERVAL '12 hours',   CURRENT_TIMESTAMP - INTERVAL '6 hours'),
    (14, 14, 60,  9,  'IN_PROGRESS', false, false, 0,  CURRENT_TIMESTAMP - INTERVAL '1 hour',     CURRENT_TIMESTAMP - INTERVAL '10 minutes'),
    (15, 15, 40,  7,  'COMPLETED',   true,  true,  40, CURRENT_TIMESTAMP - INTERVAL '2 days',     CURRENT_TIMESTAMP - INTERVAL '1 day'),
    (16, 16, 50,  21, 'IN_PROGRESS', false, false, 0,  CURRENT_TIMESTAMP - INTERVAL '3 hours',    CURRENT_TIMESTAMP - INTERVAL '1 hour'),
    (17, 17, 55,  30, 'COMPLETED',   true,  true,  0,  CURRENT_TIMESTAMP - INTERVAL '4 hours',    CURRENT_TIMESTAMP - INTERVAL '2 hours'),
    (18, 18, 12,  8,  'COMPLETED',   true,  true,  12, CURRENT_TIMESTAMP - INTERVAL '2 days',     CURRENT_TIMESTAMP - INTERVAL '1 day'),
    (19, 19, 45,  15, 'IN_PROGRESS', false, false, 0,  CURRENT_TIMESTAMP - INTERVAL '30 minutes', CURRENT_TIMESTAMP - INTERVAL '10 minutes'),
    (20, 20, 35,  10, 'COMPLETED',   true,  true,  35, CURRENT_TIMESTAMP - INTERVAL '2 days',     CURRENT_TIMESTAMP - INTERVAL '1 day'),
    (21, 21, 75,  1,  'COMPLETED',   true,  true,  0,  CURRENT_TIMESTAMP - INTERVAL '4 hours',    CURRENT_TIMESTAMP - INTERVAL '2 hours'),
    (22, 22, 18,  14, 'COMPLETED',   true,  true,  18, CURRENT_TIMESTAMP - INTERVAL '2 days',     CURRENT_TIMESTAMP - INTERVAL '1 day'),
    (23, 23, 70,  12, 'IN_PROGRESS', false, false, 0,  CURRENT_TIMESTAMP - INTERVAL '2 hours',    CURRENT_TIMESTAMP - INTERVAL '30 minutes'),
    (24, 24, 65,  11, 'COMPLETED',   true,  true,  65, CURRENT_TIMESTAMP - INTERVAL '3 hours',    CURRENT_TIMESTAMP - INTERVAL '1 hour'),
    (25, 31, 110, 22, 'IN_PROGRESS', false, false, 0,  CURRENT_TIMESTAMP - INTERVAL '1 hour',     CURRENT_TIMESTAMP - INTERVAL '25 minutes'),
    (26, 32, 28,  6,  'IN_PROGRESS', false, false, 0,  CURRENT_TIMESTAMP - INTERVAL '45 minutes', CURRENT_TIMESTAMP - INTERVAL '15 minutes'),
    (27, 33, 75,  24, 'IN_PROGRESS', false, false, 0,  CURRENT_TIMESTAMP - INTERVAL '2 hours',    CURRENT_TIMESTAMP - INTERVAL '40 minutes');

INSERT INTO transport_units (barcode, order_id, replenishment_id)
VALUES
    ('TU100001', NULL, NULL),
    ('TU100002', 1, NULL),
    ('TU100003', NULL, NULL),
    ('TU100004', NULL, 1),
    ('TU100005', 2, NULL),
    ('TU100006', NULL, NULL),
    ('TU100007', 3, NULL),
    ('TU100008', NULL, 2),
    ('TU100009', NULL, NULL),
    ('TU100010', 5, NULL),
    ('TU100011', 4, NULL),
    ('TU100012', NULL, NULL),
    ('TU100013', 6, NULL),
    ('TU100014', 7, NULL),
    ('TU100015', NULL, NULL),
    ('TU100016', NULL, 3),
    ('TU100017', 8, NULL),
    ('TU100018', NULL, NULL),
    ('TU100019', 9, NULL),
    ('TU100020', NULL, 4),
    ('TU100021', NULL, NULL),
    ('TU100022', 10, NULL),
    ('TU100023', 11, NULL),
    ('TU100024', NULL, NULL),
    ('TU100025', 12, NULL),
    ('TU100026', 13, NULL),
    ('TU100027', NULL, 5),
    ('TU100028', NULL, NULL),
    ('TU100029', 14, NULL),
    ('TU100030', NULL, NULL),
    ('TU100031', 15, NULL),
    ('TU100032', 16, NULL),
    ('TU100033', NULL, NULL),
    ('TU100034', 17, NULL),
    ('TU100035', NULL, 6),
    ('TU100036', NULL, NULL),
    ('TU100037', 18, NULL),
    ('TU100038', 19, NULL),
    ('TU100039', NULL, NULL),
    ('TU100040', 20, NULL),
    ('TU100041', NULL, 7),
    ('TU100042', NULL, NULL),
    ('TU100043', 21, NULL),
    ('TU100044', NULL, NULL),
    ('TU100045', 22, NULL),
    ('TU100046', 23, NULL),
    ('TU100047', NULL, NULL),
    ('TU100048', 24, NULL),
    ('TU100049', 25, NULL),
    ('TU100050', NULL, NULL);

SELECT setval('users_sequence', (SELECT MAX(id) FROM users));
SELECT setval('categories_sequence', (SELECT MAX(id) FROM categories));
SELECT setval('products_sequence', (SELECT MAX(id) FROM products));
SELECT setval('locations_sequence', (SELECT MAX(id) FROM locations));
SELECT setval('stocks_sequence', (SELECT MAX(id) FROM stocks));
SELECT setval('inventory_sequence', (SELECT MAX(id) FROM inventory_history));
SELECT setval('tasks_sequence', (SELECT MAX(id) FROM tasks));
SELECT setval('replenishments_sequence', (SELECT MAX(id) FROM replenishments));
SELECT setval('orders_sequence', (SELECT MAX(id) FROM orders));
SELECT setval('order_lines_sequence', (SELECT MAX(id) FROM order_lines));
SELECT setval('allocations_sequence', (SELECT MAX(id) FROM allocations));
SELECT setval('transport_units_sequence', (SELECT MAX(id) FROM transport_units));

/*
 * Sleuth Kit Data Model
 *
 * Copyright 2021 Basis Technology Corp.
 * Contact: carrier <at> sleuthkit <dot> org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.sleuthkit.datamodel;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * Tests TSK Attribute apis.
 * Under test are the following
 * - File Attribute
 *
 */
public class AttributeTest {

	private static final Logger LOGGER = Logger.getLogger(AttributeTest.class.getName());

	private static SleuthkitCase caseDB;

	private final static String TEST_DB = "AttributeApiTest.db";


	private static String dbPath = null;
	private static FileSystem fs = null;

	public AttributeTest (){

	}

	@BeforeClass
	public static void setUpClass() {
		String tempDirPath = System.getProperty("java.io.tmpdir");
		try {
			dbPath = Paths.get(tempDirPath, TEST_DB).toString();

			// Delete the DB file, in case
			java.io.File dbFile = new java.io.File(dbPath);
			dbFile.delete();
			if (dbFile.getParentFile() != null) {
				dbFile.getParentFile().mkdirs();
			}

			// Create new case db
			caseDB = SleuthkitCase.newCase(dbPath);

			SleuthkitCase.CaseDbTransaction trans = caseDB.beginTransaction();

			Image img = caseDB.addImage(TskData.TSK_IMG_TYPE_ENUM.TSK_IMG_TYPE_DETECT, 512, 1024, "", Collections.emptyList(), "America/NewYork", null, null, null, "first", trans);

			fs = caseDB.addFileSystem(img.getId(), 0, TskData.TSK_FS_TYPE_ENUM.TSK_FS_TYPE_RAW, 0, 0, 0, 0, 0, "", trans);

			trans.commit();


			System.out.println("Attributes Test DB created at: " + dbPath);
		} catch (TskCoreException ex) {
			LOGGER.log(Level.SEVERE, "Failed to create new case", ex);
		}
	}


	@AfterClass
	public static void tearDownClass() {

	}

	@Before
	public void setUp() {
	}

	@After
	public void tearDown() {
	}

	@Test
	public void fileAttributeTests() throws TskCoreException {

		String testMD5 = "c67017ead6356b987b30536d35e8f562";
		List<Attribute> fileAttributes = new ArrayList<>();
		fileAttributes.add(new Attribute(new BlackboardAttribute.Type(BlackboardAttribute.ATTRIBUTE_TYPE.TSK_DATETIME_ACCESSED), 1611233915l));

		List<Attribute> fileAttributes2 = new ArrayList<>();
		fileAttributes2.add(new Attribute(new BlackboardAttribute.Type(BlackboardAttribute.ATTRIBUTE_TYPE.TSK_SSID), "S-1-15-3443-2233"));


		long dataSourceObjectId = fs.getDataSource().getId();
		
		SleuthkitCase.CaseDbTransaction trans = caseDB.beginTransaction();

		// Add a root folder
		FsContent root = caseDB.addFileSystemFile(dataSourceObjectId, fs.getId(), "", 0, 0,
				TskData.TSK_FS_ATTR_TYPE_ENUM.TSK_FS_ATTR_TYPE_DEFAULT, 0, TskData.TSK_FS_NAME_FLAG_ENUM.ALLOC,
				(short) 0, 200, 0, 0, 0, 0, null, null, null, false, fs, null, null, Collections.emptyList(), trans);

		// Add a dir - no attributes 
		FsContent windows = caseDB.addFileSystemFile(dataSourceObjectId, fs.getId(), "Windows", 0, 0,
				TskData.TSK_FS_ATTR_TYPE_ENUM.TSK_FS_ATTR_TYPE_DEFAULT, 0, TskData.TSK_FS_NAME_FLAG_ENUM.ALLOC,
				(short) 0, 200, 0, 0, 0, 0, null, null, null, false, root, "S-1-5-80-956008885-3418522649-1831038044-1853292631-227147846", null, Collections.emptyList(), trans);

		// Add dllhosts.exe file to the above dir
		FsContent dllhosts = caseDB.addFileSystemFile(dataSourceObjectId, fs.getId(), "dllhosts.exe", 0, 0,
				TskData.TSK_FS_ATTR_TYPE_ENUM.TSK_FS_ATTR_TYPE_DEFAULT, 0, TskData.TSK_FS_NAME_FLAG_ENUM.ALLOC,
				(short) 0, 200, 0, 0, 0, 0, testMD5, null, "Applicatione/Exe", true, windows, "S-1-5-32-544", null, fileAttributes, trans);

		// add another no attribute file to the same folder
		FsContent _nofile = caseDB.addFileSystemFile(dataSourceObjectId, fs.getId(), "nofile.exe", 0, 0,
				TskData.TSK_FS_ATTR_TYPE_ENUM.TSK_FS_ATTR_TYPE_DEFAULT, 0, TskData.TSK_FS_NAME_FLAG_ENUM.ALLOC,
				(short) 0, 200, 0, 0, 0, 0, null, null, "Applicatione/Exe", true, windows, null, null, Collections.emptyList(), trans);
		

		// Add additional attributes to dllhosts file - within the same transaction. 
		dllhosts.addAttributes(fileAttributes2, trans);
		
		long firstFileAttributeId  = fileAttributes.get(0).getId();
		
		assertEquals("Assert that the first file attribute has a db generated id",true, firstFileAttributeId > 0);
		
		trans.commit();
 
		assertEquals(2, dllhosts.getAttributes().size());
		assertEquals(firstFileAttributeId, dllhosts.getAttributes().get(0).getId());
		
		
		// Lookup the file by Md5 and assert it has 2 attributes 
		List<AbstractFile> matchingFiles = caseDB.findFilesByMd5(testMD5);
		assertEquals(1, matchingFiles.size());
		assertEquals(2, matchingFiles.get(0).getAttributes().size());
		assertEquals(firstFileAttributeId, matchingFiles.get(0).getAttributes().get(0).getId());

		List<AbstractFile> nofile = caseDB.findFiles(fs.getDataSource(), "nofile.exe");
		assertEquals(1, nofile.size());
		assertEquals(0, nofile.get(0).getAttributes().size());
 

	}
}

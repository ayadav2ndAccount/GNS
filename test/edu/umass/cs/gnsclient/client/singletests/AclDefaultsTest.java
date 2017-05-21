/*
 *
 *  Copyright (c) 2015 University of Massachusetts
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you
 *  may not use this file except in compliance with the License. You
 *  may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 *
 *  Initial developer(s): Westy
 *
 */
package edu.umass.cs.gnsclient.client.singletests;

import edu.umass.cs.gnsclient.client.GNSClientCommands;
import edu.umass.cs.gnscommon.AclAccessType;
import edu.umass.cs.gnsclient.client.util.GuidEntry;
import edu.umass.cs.gnsclient.client.util.GuidUtils;
import edu.umass.cs.gnsclient.client.util.JSONUtils;
import edu.umass.cs.gnsclient.jsonassert.JSONAssert;

import edu.umass.cs.gnscommon.GNSProtocol;
import edu.umass.cs.gnscommon.ResponseCode;
import edu.umass.cs.gnscommon.exceptions.client.ClientException;
import edu.umass.cs.gnscommon.utils.RandomString;
import edu.umass.cs.gnscommon.utils.ThreadUtils;
import edu.umass.cs.gnsserver.utils.DefaultGNSTest;
import edu.umass.cs.utils.Utils;
import java.io.IOException;
import java.util.Arrays;

import org.json.JSONArray;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

/**
 * This test tests all manner of ACL uses with emphasis
 * on checking the default ACL access settings.
 * 
 * The invariants  of ACL is described as follows:
 * 1. The target GUID itself is always allowed to access to all
 * of its information (i.e., every field).
 * 2. Each field is associated with a read or write white list,
 * the GUID in the read white list can read from the field, the 
 * GUID in the write white list can write into the field.
 * 3. The GUID in a higher level field's ACL white list is also
 * authorized to access to the lower level field. Here, in a JSON, 
 * a higher level field is the field whose value is another JSON
 * that contains the lower level field. 
 * 4. The GUIDs does not fall in the rule 1-3 are not allowed to
 * access to target GUID's field.
 * 
 * Here is an example for the rules described above.
 * Let's denote the original JSON as follows:
 * {
 * 	"layer1":
 * 			{
 * 				"layer2":"some value"
 * 			}
 * 	"another field": "some value"
 * }
 * Let's denote the target GUID's ACL as follows:
 * {
 * 	"_GNS_ACL":{
 * 		"READ_WHITELIST":{
 * 		"+ALL+":[guid1],
 * 		"layer1":[guid2],
 * 		"layer1.layer2":[guid3]
 * 		}
 * 	}
 * }
 * 
 * In this example, guid1 is allowed to access all the field, including "another field",
 * as it is in the root's ACL whitelist (i.e., "+ALL+"). While guid2 is only in the whitelist
 * of "layer1", therefore, it is only allowed to access to all the fields in "layer1"(i.e.,
 * "layer1" as well as "layer2"). Finally guid3 is only allowed to access to "layer2".
 * Any other guid is not allowed to access to any of these fields.
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class AclDefaultsTest extends DefaultGNSTest {

  private static GNSClientCommands clientCommands = null;
  private static GuidEntry masterGuid;
  private static GuidEntry westyEntry;
  private static GuidEntry samEntry;
  private static GuidEntry barneyEntry;

  /**
   *
   */
  public AclDefaultsTest() {
    if (clientCommands == null) {
      try {
        clientCommands = new GNSClientCommands();
        clientCommands.setForceCoordinatedReads(true);
      } catch (IOException e) {
        Utils.failWithStackTrace("Exception creating client: ", e);
      }
    }
  }

  /**
   *
   */
  @Test
  public void test_100_CreateMasterGuid() {
    try {
      masterGuid = GuidUtils.getGUIDKeys(globalAccountName);
    } catch (Exception e) {
      Utils.failWithStackTrace("Exception while creating guid: ", e);
    }
  }

  private static final String TEST_FIELD_NAME = "testField";

  /**
   *
   */
  @Test
  public void test_101_ACLCreateField() {
    try {
      clientCommands.fieldCreateOneElementList(masterGuid.getGuid(), TEST_FIELD_NAME, "testValue", masterGuid);
    } catch (Exception e) {
      Utils.failWithStackTrace("Exception while creating fields in ACLCreateFields: ", e);

    }
  }

  //
  // Start with some simple tests to insure that basic ACL mechanics work
  //
  /**
   * Add the ALL_GUID to GNSProtocol.ENTIRE_RECORD.toString() if it's not there already.
   */
  @Test
  public void test_110_ACLMaybeAddAllFields() {
    try {
      if (!JSONUtils.JSONArrayToArrayList(clientCommands.aclGet(AclAccessType.READ_WHITELIST, masterGuid,
              GNSProtocol.ENTIRE_RECORD.toString(), masterGuid.getGuid()))
              .contains(GNSProtocol.ALL_GUIDS.toString())) {
        clientCommands.aclAdd(AclAccessType.READ_WHITELIST, masterGuid,
                GNSProtocol.ENTIRE_RECORD.toString(),
                GNSProtocol.ALL_GUIDS.toString());
      }
    } catch (ClientException | IOException | JSONException e) {
      Utils.failWithStackTrace("Exception while checking for ALL_FIELDS in ACLMaybeAddAllFields: ", e);
    }
  }

  /**
   *
   */
  @Test
  public void test_111_ACLCheckForAllFieldsPass() {
    try {
      ThreadUtils.sleep(100);
      JSONArray expected = new JSONArray(Arrays.asList(GNSProtocol.ALL_GUIDS.toString()));
      JSONAssert.assertEquals(expected,
              clientCommands.aclGet(AclAccessType.READ_WHITELIST, masterGuid,
                      GNSProtocol.ENTIRE_RECORD.toString(), masterGuid.getGuid()), true);
    } catch (ClientException | IOException | JSONException e) {
      Utils.failWithStackTrace("Exception while checking ALL_FIELDS in ACLCheckForAllFieldsPass: ", e);
    }
  }

  /**
   *
   */
  @Test
  public void test_112_ACLRemoveAllFields() {
    try {
      // remove default read access for this test
      clientCommands.aclRemove(AclAccessType.READ_WHITELIST, masterGuid,
              GNSProtocol.ENTIRE_RECORD.toString(), GNSProtocol.ALL_GUIDS.toString());
    } catch (ClientException | IOException e) {
      Utils.failWithStackTrace("Exception while removing ACL in ACLRemoveAllFields: ", e);

    }
  }

  /**
   *
   */
  @Test
  public void test_113_ACLCheckForAllFieldsMissing() {
    try {
      JSONArray expected = new JSONArray();
      JSONAssert.assertEquals(expected,
              clientCommands.aclGet(AclAccessType.READ_WHITELIST, masterGuid,
                      GNSProtocol.ENTIRE_RECORD.toString(), masterGuid.getGuid()), true);
    } catch (ClientException | IOException | JSONException e) {
      Utils.failWithStackTrace("Exception while checking ALL_FIELDS in ACLCheckForAllFieldsMissing: ", e);

    }
  }

  /**
   *
   */
  @Test
  public void test_114_CheckAllFieldsAcl() {
    try {
      Assert.assertTrue(clientCommands.fieldAclExists(AclAccessType.READ_WHITELIST, masterGuid, GNSProtocol.ENTIRE_RECORD.toString()));
    } catch (ClientException | IOException e) {
      Utils.failWithStackTrace("Exception in CheckAllFieldsAcl: ", e);
    }
  }

  /**
   *
   */
  @Test
  public void test_115_DeleteAllFieldsAcl() {
    try {
      clientCommands.fieldDeleteAcl(AclAccessType.READ_WHITELIST, masterGuid, GNSProtocol.ENTIRE_RECORD.toString());
    } catch (ClientException | IOException e) {
      Utils.failWithStackTrace("Exception in DeleteAllFieldsAcl: ", e);
    }
  }

  /**
   *
   */
  @Test
  public void test_116_CheckAllFieldsAclGone() {
    try {
      Assert.assertFalse(clientCommands.fieldAclExists(AclAccessType.READ_WHITELIST, masterGuid, GNSProtocol.ENTIRE_RECORD.toString()));
    } catch (ClientException | IOException e) {
      Utils.failWithStackTrace("Exception in CheckAllFieldsAclGone: ", e);
    }
  }

  /**
   *
   */
  @Test
  public void test_120_CreateAcl() {
    try {
      clientCommands.fieldCreateAcl(AclAccessType.READ_WHITELIST, masterGuid, TEST_FIELD_NAME);
    } catch (ClientException | IOException e) {
      Utils.failWithStackTrace("Exception CreateAcl while creating ACL field: ", e);
    }
  }

  /**
   *
   */
  @Test
  public void test_121_CheckAcl() {
    try {
      Assert.assertTrue(clientCommands.fieldAclExists(AclAccessType.READ_WHITELIST, masterGuid, TEST_FIELD_NAME));
    } catch (ClientException | IOException e) {
      Utils.failWithStackTrace("Exception CheckAcl: ", e);
    }
  }

  /**
   *
   */
  @Test
  public void test_122_DeleteAcl() {
    try {
      clientCommands.fieldDeleteAcl(AclAccessType.READ_WHITELIST, masterGuid, TEST_FIELD_NAME);
    } catch (ClientException | IOException e) {
      Utils.failWithStackTrace("Exception in DeleteAcl: ", e);
    }
  }

  /**
   *
   */
  @Test
  public void test_123_CheckAclGone() {
    try {
      Assert.assertFalse(clientCommands.fieldAclExists(AclAccessType.READ_WHITELIST, masterGuid, TEST_FIELD_NAME));
    } catch (ClientException | IOException e) {
      Utils.failWithStackTrace("Exception in CheckAclGonewhile: ", e);
    }
  }

  /**
   * Create guids for ACL tests.
   */
  @Test
  public void test_130_ACLCreateGuids() {
    try {
      westyEntry = clientCommands.guidCreate(masterGuid, "westy" + RandomString.randomString(12));
      samEntry = clientCommands.guidCreate(masterGuid, "sam" + RandomString.randomString(12));
    } catch (ClientException | IOException e) {
      Utils.failWithStackTrace("Exception registering guids in ACLCreateGuids: ", e);
    }
  }

  /**
   *
   */
  @Test
  public void test_131_ACLRemoveAllFields() {
    try {
      // remove default read access for this test
      clientCommands.aclRemove(AclAccessType.READ_WHITELIST, westyEntry,
              GNSProtocol.ENTIRE_RECORD.toString(), GNSProtocol.ALL_GUIDS.toString());
      clientCommands.aclRemove(AclAccessType.READ_WHITELIST, samEntry,
              GNSProtocol.ENTIRE_RECORD.toString(), GNSProtocol.ALL_GUIDS.toString());
    } catch (ClientException | IOException e) {
      Utils.failWithStackTrace("Exception while removing ACL in ACLRemoveAllFields: ", e);

    }
  }

  /**
   *
   */
  @Test
  public void test_132_ACLCreateFields() {
    try {
      clientCommands.fieldUpdate(westyEntry.getGuid(), "environment", "work", westyEntry);
      clientCommands.fieldUpdate(westyEntry.getGuid(), "ssn", "000-00-0000", westyEntry);
      clientCommands.fieldUpdate(westyEntry.getGuid(), "password", "666flapJack", westyEntry);
      clientCommands.fieldUpdate(westyEntry.getGuid(), "address", "100 Hinkledinkle Drive", westyEntry);
    } catch (IOException | ClientException e) {
      Utils.failWithStackTrace("Exception while creating fields in ACLCreateFields: ", e);

    }
  }

  /**
   *
   */
  @Test
  public void test_135_ACLMaybeAddAllFieldsForMaster() {
    try {
      if (!JSONUtils.JSONArrayToArrayList(clientCommands.aclGet(AclAccessType.READ_WHITELIST, westyEntry,
              GNSProtocol.ENTIRE_RECORD.toString(), westyEntry.getGuid()))
              .contains(masterGuid.getGuid())) {
        clientCommands.aclAdd(AclAccessType.READ_WHITELIST, westyEntry,
                GNSProtocol.ENTIRE_RECORD.toString(),
                masterGuid.getGuid());
      }
    } catch (ClientException | IOException | JSONException e) {
      Utils.failWithStackTrace("Exception while checking for ALL_FIELDS in ACLMaybeAddAllFields: ", e);

    }
  }

  /**
   *
   */
  @Test
  public void test_136_ACLMasterReadAllFields() {
    try {
      JSONObject expected = new JSONObject();
      expected.put("environment", "work");
      expected.put("password", "666flapJack");
      expected.put("ssn", "000-00-0000");
      expected.put("address", "100 Hinkledinkle Drive");
      JSONObject actual = new JSONObject(clientCommands.fieldRead(westyEntry.getGuid(),
              GNSProtocol.ENTIRE_RECORD.toString(), masterGuid));
      JSONAssert.assertEquals(expected, actual, true);
    } catch (JSONException | ClientException | IOException e) {
      Utils.failWithStackTrace("Exception while reading all fields in ACLReadAllFields: ", e);

    }
  }

  /**
   *
   */
  @Test
  public void test_137_ACLReadMyFields() {
    try {
      // read my own field
      Assert.assertEquals("work",
              clientCommands.fieldRead(westyEntry.getGuid(), "environment", westyEntry));
      // read another one of my fields field
      Assert.assertEquals("000-00-0000",
              clientCommands.fieldRead(westyEntry.getGuid(), "ssn", westyEntry));

    } catch (ClientException | IOException e) {
      Utils.failWithStackTrace("Exception while reading fields in ACLReadMyFields: ", e);

    }
  }

  /**
   *
   */
  @Test
  public void test_138_ACLNotReadOtherGuidAllFieldsTest() {
    try {
      try {
        String result = clientCommands.fieldRead(westyEntry.getGuid(), GNSProtocol.ENTIRE_RECORD.toString(), samEntry);
        Utils.failWithStackTrace("Result of read of all of westy's fields by sam is " + result
                + " which is wrong because it should have been rejected.");
      } catch (ClientException e) {
      }
    } catch (Exception e) {
      Utils.failWithStackTrace("Exception while reading fields in ACLNotReadOtherGuidAllFieldsTest: ", e);

    }
  }

  /**
   *
   */
  @Test
  public void test_139_ACLNotReadOtherGuidFieldTest() {
    try {
      try {
        String result = clientCommands.fieldRead(westyEntry.getGuid(), "environment",
                samEntry);
        Utils.failWithStackTrace("Result of read of westy's environment by sam is " + result
                + " which is wrong because it should have been rejected.");
      } catch (ClientException e) {
      }
    } catch (Exception e) {
      Utils.failWithStackTrace("Exception while reading fields in ACLNotReadOtherGuidFieldTest: ", e);

    }
  }

  /**
   *
   */
  @Test
  public void test_140_AddACLTest() {
    try {
      try {
        clientCommands.aclAdd(AclAccessType.READ_WHITELIST, westyEntry, "environment", samEntry.getGuid());
      } catch (ClientException | IOException e) {
        Utils.failWithStackTrace("Exception adding Sam to Westy's readlist: ", e);

      }
    } catch (Exception e) {
      Utils.failWithStackTrace("Exception when we were not expecting it in ACLPartOne: ", e);

    }
  }

  /**
   *
   */
  @Test
  public void test_141_CheckACLTest() {
    try {
      try {
        Assert.assertEquals("work", clientCommands.fieldRead(westyEntry.getGuid(), "environment", samEntry));
      } catch (ClientException | IOException e) {
        Utils.failWithStackTrace("Exception while Sam reading Westy's field: ", e);

      }
    } catch (Exception e) {
      Utils.failWithStackTrace("Exception when we were not expecting it in ACLPartOne: ", e);

    }
  }

  /**
   *
   */
  @Test
  public void test_142_ACLCreateAnotherGuid() {
    try {
      String barneyName = "barney" + RandomString.randomString(12);
      try {
        clientCommands.lookupGuid(barneyName);
        Utils.failWithStackTrace(barneyName + " entity should not exist");
      } catch (ClientException e) {
        // normal result
      } catch (IOException e) {
        Utils.failWithStackTrace("Exception looking up Barney: ", e);
      }
      barneyEntry = clientCommands.guidCreate(masterGuid, barneyName);
    } catch (ClientException | IOException e) {
      Utils.failWithStackTrace("Exception when we were not expecting it in ACLPartTwo: ", e);

    }
  }

  /**
   *
   */
  @Test
  public void test_143_ACLAdjustACL() {
    try {
      // remove default read access for this test
      clientCommands.aclRemove(AclAccessType.READ_WHITELIST, barneyEntry,
              GNSProtocol.ENTIRE_RECORD.toString(), GNSProtocol.ALL_GUIDS.toString());
    } catch (ClientException | IOException e) {
      Utils.failWithStackTrace("Exception when we were not expecting it in ACLPartTwo: ", e);

    }
  }

  /**
   *
   */
  @Test
  public void test_144_ACLCreateFields() {
    try {
      // remove default read access for this test
      clientCommands.fieldUpdate(barneyEntry.getGuid(), "cell", "413-555-1234", barneyEntry);
      clientCommands.fieldUpdate(barneyEntry.getGuid(), "address", "100 Main Street", barneyEntry);
    } catch (IOException | ClientException e) {
      Utils.failWithStackTrace("Exception when we were not expecting it in ACLPartTwo: ", e);

    }
  }

  /**
   *
   */
  @Test
  public void test_145_ACLUpdateACL() {
    try {
      try {
        // let anybody read barney's cell field
        clientCommands.aclAdd(AclAccessType.READ_WHITELIST, barneyEntry, "cell",
                GNSProtocol.ALL_GUIDS.toString());
      } catch (ClientException | IOException e) {
        Utils.failWithStackTrace("Exception creating ALL_GUIDS access for Barney's cell: ", e);

      }
    } catch (Exception e) {
      Utils.failWithStackTrace("Exception when we were not expecting it in ACLPartTwo: ", e);

    }
  }

  /**
   *
   */
  @Test
  public void test_146_ACLTestReadsOne() {
    try {
      try {
        Assert.assertEquals("413-555-1234",
                clientCommands.fieldRead(barneyEntry.getGuid(), "cell", samEntry));
      } catch (ClientException | IOException e) {
        Utils.failWithStackTrace("Exception while Sam reading Barney' cell: ", e);

      }
    } catch (Exception e) {
      Utils.failWithStackTrace("Exception when we were not expecting it in ACLPartOne: ", e);

    }
  }

  /**
   *
   */
  @Test
  public void test_147_ACLTestReadsTwo() {
    try {
      try {
        Assert.assertEquals("413-555-1234",
                clientCommands.fieldRead(barneyEntry.getGuid(), "cell", westyEntry));
      } catch (ClientException | IOException e) {
        Utils.failWithStackTrace("Exception while Westy reading Barney' cell: ", e);

      }
    } catch (Exception e) {
      Utils.failWithStackTrace("Exception when we were not expecting it in ACLTestReadsTwo: ", e);

    }
  }

  /**
   *
   */
  @Test
  public void test_148_ACLTestReadsThree() {
    try {
      try {
        String result = clientCommands.fieldRead(barneyEntry.getGuid(), "address",
                samEntry);
        Utils.failWithStackTrace("Result of read of barney's address by sam is " + result
                + " which is wrong because it should have been rejected.");
      } catch (ClientException e) {
        if (e.getCode() == ResponseCode.ACCESS_ERROR) {
          System.out.print("This was expected for null querier trying to ReadUnsigned "
                  + barneyEntry.getGuid()
                  + "'s address: "
                  + e);
        }
      } catch (IOException e) {
        Utils.failWithStackTrace("Exception while Sam reading Barney' address: ", e);

      }

    } catch (Exception e) {
      Utils.failWithStackTrace("Exception when we were not expecting it in ACLTestReadsThree: ", e);

    }
  }

  /**
   *
   */
  @Test
  public void test_149_ACLALLFields() {
    String superUserName = "superuser" + RandomString.randomString(12);
    try {
      try {
        clientCommands.lookupGuid(superUserName);
        Utils.failWithStackTrace(superUserName + " entity should not exist");
      } catch (ClientException e) {
      }

      GuidEntry superuserEntry = clientCommands.guidCreate(masterGuid, superUserName);

      // let superuser read any of barney's fields
      clientCommands.aclAdd(AclAccessType.READ_WHITELIST, barneyEntry,
              GNSProtocol.ENTIRE_RECORD.toString(), superuserEntry.getGuid());

      Assert.assertEquals("413-555-1234",
              clientCommands.fieldRead(barneyEntry.getGuid(), "cell", superuserEntry));
      Assert.assertEquals("100 Main Street",
              clientCommands.fieldRead(barneyEntry.getGuid(), "address", superuserEntry));
      try {
        clientCommands.guidRemove(masterGuid, superuserEntry.getGuid());
      } catch (IOException | ClientException e) {
        Utils.failWithStackTrace("Exception while deleting superuserEntry in ACLALLFields: ", e);
      }
    } catch (IOException | ClientException e) {
      Utils.failWithStackTrace("Exception when we were not expecting it in ACLALLFields: ", e);
    }

  }

  /**
   *
   */
  @Test
  public void test_150_ACLCreateDeeperField() {
    try {
      try {
        clientCommands.fieldUpdate(westyEntry.getGuid(), "test.deeper.field", "fieldValue", westyEntry);
      } catch (IOException | ClientException e) {
        Utils.failWithStackTrace("Problem updating field: ", e);

      }
    } catch (Exception e) {
      Utils.failWithStackTrace("Exception when we were not expecting it ACLCreateDeeperField: ", e);

    }
  }

  /**
   *
   */
  @Test
  public void test_151_ACLAddDeeperFieldACL() {
    try {
      try {
        // Create an empty ACL, effectively disabling access except by the guid itself.
        clientCommands.fieldCreateAcl(AclAccessType.READ_WHITELIST, westyEntry, "test.deeper.field");
      } catch (ClientException | IOException e) {
        Utils.failWithStackTrace("Problem adding acl: ", e);

      }
    } catch (Exception e) {
      Utils.failWithStackTrace("Exception when we were not expecting it ACLCreateDeeperField: ", e);

    }
  }

  /**
   *
   */
  @Test
  public void test_152_ACLCheckDeeperFieldACLExists() {
    try {
      try {
        Assert.assertTrue(clientCommands.fieldAclExists(AclAccessType.READ_WHITELIST, westyEntry, "test.deeper.field"));
      } catch (ClientException | IOException e) {
        Utils.failWithStackTrace("Problem reading acl: ", e);

      }
    } catch (Exception e) {
      Utils.failWithStackTrace("Exception when we were not expecting it ACLCreateDeeperField: ", e);

    }
  }

  // This should pass even though the ACL for test.deeper.field is empty because you
  // can always read your own fields.
  /**
   *
   */
  @Test
  public void test_153_ACLReadDeeperFieldSelf() {
    try {
      try {
        Assert.assertEquals("fieldValue", clientCommands.fieldRead(westyEntry.getGuid(), "test.deeper.field", westyEntry));
      } catch (ClientException | IOException e) {
        Utils.failWithStackTrace("Problem adding read field: ", e);

      }
    } catch (Exception e) {
      Utils.failWithStackTrace("Exception when we were not expecting it ACLCreateDeeperField: ", e);

    }
  }

  // This should fail because the ACL for test.deeper.field is empty.
  /**
   *
   */
  @Test
  public void test_154_ACLReadDeeperFieldOtherFail() {
    try {
      try {
        Assert.assertEquals("fieldValue", clientCommands.fieldRead(westyEntry.getGuid(), "test.deeper.field", samEntry));
        Utils.failWithStackTrace("This read should have failed.");
      } catch (ClientException | IOException e) {
      }
    } catch (Exception e) {
      Utils.failWithStackTrace("Exception when we were not expecting it ACLCreateDeeperField: ", e);

    }
  }

  // This should fail because the ACL for test.deeper.field is empty.
  /**
   *
   */
  @Test
  public void test_156_ACLReadShallowFieldOtherFail() {
    try {
      try {
        Assert.assertEquals("fieldValue", clientCommands.fieldRead(westyEntry.getGuid(), "test.deeper", samEntry));
        Utils.failWithStackTrace("This read should have failed.");
      } catch (ClientException | IOException e) {
      }
    } catch (Exception e) {
      Utils.failWithStackTrace("Exception when we were not expecting it ACLCreateDeeperField: ", e);

    }
  }

  /**
   *
   */
  @Test
  public void test_157_AddAllRecordACL() {
    try {
      clientCommands.aclAdd(AclAccessType.READ_WHITELIST, westyEntry, "test", GNSProtocol.ALL_GUIDS.toString());
    } catch (ClientException | IOException e) {
      Utils.failWithStackTrace("Exception when we were not expecting it ACLCreateDeeperField: ", e);

    }
  }

  /**
   * Since GNSProtocol.ALL_GUIDS.toString() is at the root white list, it means
   * every one is allowed to access to all the fields of this GUID.
   * So this test should succeed. 
   */
  // This should still fail because the ACL for test.deeper.field is empty even though test 
  // now has an GNSProtocol.ALL_GUIDS.toString() at the root (this is different than the old model).
  @Test
  public void test_158_ACLReadDeeperFieldOtherFail() {
    try {
      try {
        Assert.assertEquals("fieldValue", clientCommands.fieldRead(westyEntry.getGuid(), "test.deeper.field", samEntry));
      } catch (ClientException | IOException e) {
    	  Utils.failWithStackTrace("This read should succeed: ",e);
      }
    } catch (Exception e) {
      Utils.failWithStackTrace("Exception when we were not expecting it ACLCreateDeeperField: ", e);

    }
  }

  /**
   *
   */
  @Test
  public void test_160_ACLDefaultsTestCleanup() {
    try {
      clientCommands.guidRemove(masterGuid, barneyEntry.getGuid());
      clientCommands.guidRemove(masterGuid, westyEntry.getGuid());
      clientCommands.guidRemove(masterGuid, samEntry.getGuid());
    } catch (ClientException | IOException e) {
      Utils.failWithStackTrace("Exception during cleanup: " + e);
    }
  }
}

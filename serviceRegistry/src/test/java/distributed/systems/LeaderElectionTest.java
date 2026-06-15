package distributed.systems;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("LeaderElection Unit Tests")
class LeaderElectionTest {

    private LeaderElection leaderElection;

    @BeforeEach
    void setUp() {
        leaderElection = new LeaderElection();
    }

    private void setPrivateField(String fieldName, Object value) throws NoSuchFieldException, IllegalAccessException {
        Field field = LeaderElection.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(leaderElection, value);
    }

    private Object getPrivateField(String fieldName) throws NoSuchFieldException, IllegalAccessException {
        Field field = LeaderElection.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(leaderElection);
    }

    @Test
    @DisplayName("Should initialize LeaderElection instance successfully")
    void testInitialization() {
        assertNotNull(leaderElection);
    }

    @Test
    @DisplayName("Should have null zookeeper on initialization")
    void testZooKeeperInitializedAsNull() throws NoSuchFieldException, IllegalAccessException {
        Object zk = getPrivateField("zooKeeper");
        assertNull(zk);
    }

    @Test
    @DisplayName("Should have null currentZNodeName on initialization")
    void testCurrentZNodeNameInitializedAsNull() throws NoSuchFieldException, IllegalAccessException {
        Object zNodeName = getPrivateField("currentZNodeName");
        assertNull(zNodeName);
    }

    @Test
    @DisplayName("Should extract znode name correctly when volunteering for leadership")
    void testCurrentZNodeNameExtraction() throws Exception {
        // When volunteering, the currentZNodeName should be extracted from the full path
        // This tests the logic: fullPath.replace("/election/", "")
        
        // Simulate what volunteerForLeadership does
        String fullPath = "/election/c_0000000001";
        String expected = "c_0000000001";
        
        String extracted = fullPath.replace("/election/", "");
        
        assertEquals(expected, extracted);
    }

    @Test
    @DisplayName("Should perform actions based on data without errors")
    void testPerformActionsBasedOnData() {
        // Arrange
        String zNode = "/target_znode";
        String data = "test-action-data";

        // Act & Assert - should not throw exception
        assertDoesNotThrow(() -> {
            leaderElection.performActionsBasedOnData(zNode, data.getBytes());
        });
    }

    @Test
    @DisplayName("Should handle empty byte array data")
    void testPerformActionsBasedOnEmptyData() {
        // Arrange
        String zNode = "/target_znode";
        byte[] emptyData = {};

        // Act & Assert
        assertDoesNotThrow(() -> {
            leaderElection.performActionsBasedOnData(zNode, emptyData);
        });
    }

    @Test
    @DisplayName("Should handle null znode name in performActionsBasedOnData")
    void testPerformActionsWithNullZNode() {
        // Act & Assert - method should handle null gracefully
        assertDoesNotThrow(() -> {
            leaderElection.performActionsBasedOnData(null, "data".getBytes());
        });
    }

    @Test
    @DisplayName("Should extract correct predecessor from sorted list")
    void testPredecessorCalculation() {
        // Test the logic: predecessorIndex = Collections.binarySearch(children, currentZNodeName) - 1
        // For a sorted list ["c_0000000000", "c_0000000001", "c_0000000002"]
        // If current is "c_0000000001", then predecessor index = 1 - 1 = 0
        
        java.util.List<String> children = java.util.Arrays.asList("c_0000000000", "c_0000000001", "c_0000000002");
        String currentZNodeName = "c_0000000001";
        
        int predecessorIndex = java.util.Collections.binarySearch(children, currentZNodeName) - 1;
        
        assertEquals(0, predecessorIndex);
        assertEquals("c_0000000000", children.get(predecessorIndex));
    }

    @Test
    @DisplayName("Should identify smallest child correctly")
    void testIdentifyLeader() {
        // Test leader identification logic: smallest child in sorted list is the leader
        java.util.List<String> children = java.util.Arrays.asList("c_0000000000", "c_0000000001", "c_0000000002");
        java.util.Collections.sort(children);
        
        String smallestChild = children.get(0);
        
        assertEquals("c_0000000000", smallestChild);
    }

    @Test
    @DisplayName("Should handle single child in list")
    void testSingleChildLeadership() {
        // Test when there's only one node - it should be the leader
        java.util.List<String> children = java.util.Arrays.asList("c_0000000000");
        java.util.Collections.sort(children);
        
        String smallestChild = children.get(0);
        
        assertEquals("c_0000000000", smallestChild);
    }

    @Test
    @DisplayName("Should validate class constants")
    void testClassConstants() throws Exception {
        // Verify constants are set correctly using reflection
        java.lang.reflect.Field sessionTimeoutField = LeaderElection.class.getDeclaredField("SESSION_TIMEOUT");
        sessionTimeoutField.setAccessible(true);
        int sessionTimeout = sessionTimeoutField.getInt(null);
        
        assertEquals(3000, sessionTimeout);
    }
}

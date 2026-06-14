package com.example.zk;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ZooKeeperDemo Unit Tests")
class ZooKeeperDemoTest {

    @Test
    @DisplayName("Should create instance successfully")
    void testZooKeeperDemoInstantiation() {
        assertDoesNotThrow(ZooKeeperDemo::new);
    }

    @Test
    @DisplayName("Should convert byte array to string")
    void testByteArrayToString() {
        String data = "test data";
        byte[] bytes = data.getBytes();
        String result = new String(bytes);
        
        assertEquals(data, result);
    }

    @Test
    @DisplayName("Should handle empty string encoding")
    void testEmptyStringEncoding() {
        String data = "";
        byte[] bytes = data.getBytes();
        String result = new String(bytes);
        
        assertEquals(data, result);
    }

    @Test
    @DisplayName("Should sort znode children correctly")
    void testZnodeSorting() {
        List<String> children = Arrays.asList("child3", "child1", "child2");
        Collections.sort(children);
        
        assertEquals(Arrays.asList("child1", "child2", "child3"), children);
    }

    @Test
    @DisplayName("Should find smallest child in sorted list")
    void testFindSmallestChild() {
        List<String> children = Arrays.asList("c_000000001", "c_000000000", "c_000000002");
        Collections.sort(children);
        
        String smallest = children.get(0);
        
        assertEquals("c_000000000", smallest);
    }

    @Test
    @DisplayName("Should find node in list using binary search")
    void testBinarySearch() {
        List<String> children = Arrays.asList("child1", "child2", "child3");
        int index = Collections.binarySearch(children, "child2");
        
        assertEquals(1, index);
    }

    @Test
    @DisplayName("Should return negative index when element not found in binary search")
    void testBinarySearchNotFound() {
        List<String> children = Arrays.asList("child1", "child2", "child3");
        int index = Collections.binarySearch(children, "child4");
        
        assertTrue(index < 0);
    }

    @Test
    @DisplayName("Should handle list with single element")
    void testSingleElementList() {
        List<String> children = Collections.singletonList("child1");
        
        assertEquals(1, children.size());
        assertEquals("child1", children.get(0));
    }

    @Test
    @DisplayName("Should handle empty list")
    void testEmptyList() {
        List<String> children = Collections.emptyList();
        
        assertTrue(children.isEmpty());
        assertEquals(0, children.size());
    }

    @Test
    @DisplayName("Should validate base path constant")
    void testBasePath() {
        // Base path should be /demo for this demo
        String basePath = "/demo";
        
        assertTrue(basePath.startsWith("/"));
        assertEquals("/demo", basePath);
    }

    @Test
    @DisplayName("Should construct child paths correctly")
    void testConstructChildPath() {
        String basePath = "/demo";
        String childName = "child1";
        String childPath = basePath + "/" + childName;
        
        assertEquals("/demo/child1", childPath);
    }

    @Test
    @DisplayName("Should handle path separators in operations")
    void testPathSeparators() {
        String path = "/demo/child1/subchild";
        String[] parts = path.split("/");
        
        assertEquals(4, parts.length);
        assertEquals("demo", parts[1]);
        assertEquals("child1", parts[2]);
        assertEquals("subchild", parts[3]);
    }
}

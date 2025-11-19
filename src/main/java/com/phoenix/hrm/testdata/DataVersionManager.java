package com.phoenix.hrm.testdata;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Data Version Manager for Phoenix HRM Test Automation Framework
 * 
 * Provides comprehensive data versioning and snapshot management including:
 * - Test data versioning with semantic versioning support
 * - Snapshot creation and restoration
 * - Version history tracking and comparison
 * - Branch-based versioning for parallel development
 * - Automated backup and rollback capabilities
 * - Version metadata and tagging
 * - Diff generation between versions
 * 
 * @author Phoenix HRM Test Automation Team
 * @version 4.0
 * @since Phase 4
 */
public class DataVersionManager {
    
    private static final Logger logger = LoggerFactory.getLogger(DataVersionManager.class);
    
    private final TestDataManager.TestDataConfig config;
    private final ObjectMapper objectMapper;
    private final DateTimeFormatter versionTimestamp;
    
    /**
     * Version information
     */
    public static class VersionInfo {
        private String versionId;
        private String dataName;
        private String environment;
        private String timestamp;
        private String author;
        private String description;
        private String tag;
        private String parentVersion;
        private Map<String, Object> metadata;
        
        public VersionInfo() {
            this.metadata = new HashMap<>();
            this.timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        }
        
        // Getters and setters
        public String getVersionId() { return versionId; }
        public void setVersionId(String versionId) { this.versionId = versionId; }
        
        public String getDataName() { return dataName; }
        public void setDataName(String dataName) { this.dataName = dataName; }
        
        public String getEnvironment() { return environment; }
        public void setEnvironment(String environment) { this.environment = environment; }
        
        public String getTimestamp() { return timestamp; }
        public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
        
        public String getAuthor() { return author; }
        public void setAuthor(String author) { this.author = author; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        public String getTag() { return tag; }
        public void setTag(String tag) { this.tag = tag; }
        
        public String getParentVersion() { return parentVersion; }
        public void setParentVersion(String parentVersion) { this.parentVersion = parentVersion; }
        
        public Map<String, Object> getMetadata() { return metadata; }
        public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
        
        @Override
        public String toString() {
            return String.format("Version{id='%s', data='%s', env='%s', timestamp='%s', author='%s'}", 
                versionId, dataName, environment, timestamp, author);
        }
    }
    
    /**
     * Version comparison result
     */
    public static class VersionDiff {
        private String sourceVersion;
        private String targetVersion;
        private Map<String, ChangeType> changes;
        private Map<String, Object> addedFields;
        private Map<String, Object> modifiedFields;
        private Set<String> removedFields;
        private String summary;
        
        public enum ChangeType {
            ADDED, MODIFIED, REMOVED, UNCHANGED
        }
        
        public VersionDiff(String sourceVersion, String targetVersion) {
            this.sourceVersion = sourceVersion;
            this.targetVersion = targetVersion;
            this.changes = new HashMap<>();
            this.addedFields = new HashMap<>();
            this.modifiedFields = new HashMap<>();
            this.removedFields = new HashSet<>();
        }
        
        // Getters and setters
        public String getSourceVersion() { return sourceVersion; }
        public String getTargetVersion() { return targetVersion; }
        public Map<String, ChangeType> getChanges() { return changes; }
        public Map<String, Object> getAddedFields() { return addedFields; }
        public Map<String, Object> getModifiedFields() { return modifiedFields; }
        public Set<String> getRemovedFields() { return removedFields; }
        public String getSummary() { return summary; }
        public void setSummary(String summary) { this.summary = summary; }
    }
    
    /**
     * Constructor
     */
    public DataVersionManager(TestDataManager.TestDataConfig config) {
        this.config = config;
        this.objectMapper = new ObjectMapper();
        this.versionTimestamp = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
        
        logger.debug("Initialized DataVersionManager");
    }
    
    /**
     * Create version snapshot of test data
     */
    public String createSnapshot(String dataName, String environment) {
        return createSnapshot(dataName, environment, null, null, null);
    }
    
    /**
     * Create version snapshot with metadata
     */
    public String createSnapshot(String dataName, String environment, String description, String tag, String author) {
        try {
            // Generate version ID
            String versionId = generateVersionId();
            
            // Create version info
            VersionInfo versionInfo = new VersionInfo();
            versionInfo.setVersionId(versionId);
            versionInfo.setDataName(dataName);
            versionInfo.setEnvironment(environment);
            versionInfo.setDescription(description != null ? description : "Automated snapshot");
            versionInfo.setTag(tag);
            versionInfo.setAuthor(author != null ? author : System.getProperty("user.name"));
            
            // Find latest version as parent
            String latestVersion = getLatestVersion(dataName, environment);
            if (latestVersion != null) {
                versionInfo.setParentVersion(latestVersion);
            }
            
            // Create version directory
            Path versionPath = getVersionPath(dataName, environment, versionId);
            Files.createDirectories(versionPath);
            
            // Copy current data files to version directory
            copyDataToVersion(dataName, environment, versionPath);
            
            // Save version info
            saveVersionInfo(versionPath, versionInfo);
            
            logger.info("Created data snapshot: {} for {}/{} (version: {})", 
                dataName, environment, versionId, versionId);
            
            return versionId;
            
        } catch (IOException e) {
            logger.error("Error creating snapshot for {}/{}: {}", dataName, environment, e.getMessage());
            throw new TestDataManager.TestDataException("Failed to create snapshot: " + dataName, e);
        }
    }
    
    /**
     * Restore data from version snapshot
     */
    public void restoreFromSnapshot(String dataName, String environment, String versionId) {
        try {
            Path versionPath = getVersionPath(dataName, environment, versionId);
            
            if (!Files.exists(versionPath)) {
                throw new TestDataManager.TestDataException("Version not found: " + versionId);
            }
            
            // Create backup of current data before restore
            String backupVersion = createSnapshot(dataName, environment, "Pre-restore backup", "backup", "system");
            
            // Restore data files from version
            restoreDataFromVersion(dataName, environment, versionPath);
            
            logger.info("Restored data from snapshot: {} for {}/{} (backup created: {})", 
                versionId, dataName, environment, backupVersion);
            
        } catch (IOException e) {
            logger.error("Error restoring from snapshot {}/{}/{}: {}", 
                dataName, environment, versionId, e.getMessage());
            throw new TestDataManager.TestDataException("Failed to restore from snapshot: " + versionId, e);
        }
    }
    
    /**
     * Get available versions for data
     */
    public List<String> getAvailableVersions(String dataName, String environment) {
        List<String> versions = new ArrayList<>();
        
        try {
            Path versionsPath = getVersionsBasePath(dataName, environment);
            
            if (Files.exists(versionsPath)) {
                versions = Files.list(versionsPath)
                    .filter(Files::isDirectory)
                    .map(path -> path.getFileName().toString())
                    .sorted(Collections.reverseOrder()) // Latest first
                    .collect(Collectors.toList());
            }
            
        } catch (IOException e) {
            logger.warn("Error listing versions for {}/{}: {}", dataName, environment, e.getMessage());
        }
        
        return versions;
    }
    
    /**
     * Get version information
     */
    public VersionInfo getVersionInfo(String dataName, String environment, String versionId) {
        try {
            Path versionInfoPath = getVersionPath(dataName, environment, versionId).resolve("version-info.json");
            
            if (Files.exists(versionInfoPath)) {
                return objectMapper.readValue(versionInfoPath.toFile(), VersionInfo.class);
            }
            
        } catch (IOException e) {
            logger.warn("Error reading version info for {}/{}/{}: {}", 
                dataName, environment, versionId, e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Get latest version ID
     */
    public String getLatestVersion(String dataName, String environment) {
        List<String> versions = getAvailableVersions(dataName, environment);
        return versions.isEmpty() ? null : versions.get(0);
    }
    
    /**
     * Tag a version
     */
    public void tagVersion(String dataName, String environment, String versionId, String tag) {
        VersionInfo versionInfo = getVersionInfo(dataName, environment, versionId);
        
        if (versionInfo != null) {
            versionInfo.setTag(tag);
            
            try {
                Path versionPath = getVersionPath(dataName, environment, versionId);
                saveVersionInfo(versionPath, versionInfo);
                
                logger.info("Tagged version {} as '{}'", versionId, tag);
                
            } catch (IOException e) {
                logger.error("Error tagging version {}: {}", versionId, e.getMessage());
                throw new TestDataManager.TestDataException("Failed to tag version: " + versionId, e);
            }
        } else {
            throw new TestDataManager.TestDataException("Version not found: " + versionId);
        }
    }
    
    /**
     * Find version by tag
     */
    public String findVersionByTag(String dataName, String environment, String tag) {
        List<String> versions = getAvailableVersions(dataName, environment);
        
        for (String versionId : versions) {
            VersionInfo versionInfo = getVersionInfo(dataName, environment, versionId);
            if (versionInfo != null && tag.equals(versionInfo.getTag())) {
                return versionId;
            }
        }
        
        return null;
    }
    
    /**
     * Compare two versions
     */
    public VersionDiff compareVersions(String dataName, String environment, String sourceVersionId, String targetVersionId) {
        VersionDiff diff = new VersionDiff(sourceVersionId, targetVersionId);
        
        try {
            // Load data from both versions
            Map<String, Object> sourceData = loadVersionData(dataName, environment, sourceVersionId);
            Map<String, Object> targetData = loadVersionData(dataName, environment, targetVersionId);
            
            // Compare data
            compareDataMaps(sourceData, targetData, diff);
            
            // Generate summary
            generateDiffSummary(diff);
            
            logger.debug("Compared versions {} and {} for {}/{}", 
                sourceVersionId, targetVersionId, dataName, environment);
            
        } catch (IOException e) {
            logger.error("Error comparing versions {}/{}/{}: {}", 
                dataName, environment, sourceVersionId, e.getMessage());
            throw new TestDataManager.TestDataException("Failed to compare versions", e);
        }
        
        return diff;
    }
    
    /**
     * Delete version
     */
    public void deleteVersion(String dataName, String environment, String versionId) {
        try {
            Path versionPath = getVersionPath(dataName, environment, versionId);
            
            if (Files.exists(versionPath)) {
                deleteDirectory(versionPath);
                logger.info("Deleted version: {} for {}/{}", versionId, dataName, environment);
            } else {
                throw new TestDataManager.TestDataException("Version not found: " + versionId);
            }
            
        } catch (IOException e) {
            logger.error("Error deleting version {}/{}/{}: {}", 
                dataName, environment, versionId, e.getMessage());
            throw new TestDataManager.TestDataException("Failed to delete version: " + versionId, e);
        }
    }
    
    /**
     * Clean up old versions (keep only specified number)
     */
    public void cleanupOldVersions(String dataName, String environment, int keepCount) {
        List<String> versions = getAvailableVersions(dataName, environment);
        
        if (versions.size() > keepCount) {
            List<String> versionsToDelete = versions.subList(keepCount, versions.size());
            
            for (String versionId : versionsToDelete) {
                // Don't delete tagged versions
                VersionInfo versionInfo = getVersionInfo(dataName, environment, versionId);
                if (versionInfo == null || versionInfo.getTag() == null || versionInfo.getTag().isEmpty()) {
                    deleteVersion(dataName, environment, versionId);
                }
            }
            
            logger.info("Cleaned up old versions for {}/{}, kept {} versions", 
                dataName, environment, keepCount);
        }
    }
    
    /**
     * Get version statistics
     */
    public Map<String, Object> getVersionStatistics(String dataName, String environment) {
        Map<String, Object> stats = new HashMap<>();
        
        List<String> versions = getAvailableVersions(dataName, environment);
        stats.put("totalVersions", versions.size());
        
        int taggedVersions = 0;
        Map<String, Integer> authorCount = new HashMap<>();
        
        for (String versionId : versions) {
            VersionInfo versionInfo = getVersionInfo(dataName, environment, versionId);
            if (versionInfo != null) {
                if (versionInfo.getTag() != null && !versionInfo.getTag().isEmpty()) {
                    taggedVersions++;
                }
                
                String author = versionInfo.getAuthor();
                if (author != null) {
                    authorCount.merge(author, 1, Integer::sum);
                }
            }
        }
        
        stats.put("taggedVersions", taggedVersions);
        stats.put("authorDistribution", authorCount);
        stats.put("latestVersion", getLatestVersion(dataName, environment));
        
        return stats;
    }
    
    // Private helper methods
    
    private String generateVersionId() {
        return "v" + LocalDateTime.now().format(versionTimestamp);
    }
    
    private Path getVersionsBasePath(String dataName, String environment) {
        return Paths.get(config.getDataRootPath(), config.getEnvironmentsPath(), 
                        environment, "versions", dataName);
    }
    
    private Path getVersionPath(String dataName, String environment, String versionId) {
        return getVersionsBasePath(dataName, environment).resolve(versionId);
    }
    
    private void copyDataToVersion(String dataName, String environment, Path versionPath) throws IOException {
        Path sourcePath = Paths.get(config.getDataRootPath(), config.getEnvironmentsPath(), environment);
        
        // Copy all relevant data files
        String[] extensions = {".json", ".yaml", ".yml", ".properties"};
        
        for (String ext : extensions) {
            Path sourceFile = sourcePath.resolve(dataName + ext);
            if (Files.exists(sourceFile)) {
                Path targetFile = versionPath.resolve(dataName + ext);
                Files.copy(sourceFile, targetFile, StandardCopyOption.REPLACE_EXISTING);
                logger.debug("Copied {} to version {}", sourceFile, versionPath);
            }
        }
    }
    
    private void restoreDataFromVersion(String dataName, String environment, Path versionPath) throws IOException {
        Path targetPath = Paths.get(config.getDataRootPath(), config.getEnvironmentsPath(), environment);
        
        // Restore all data files from version
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(versionPath, dataName + ".*")) {
            for (Path sourceFile : stream) {
                if (!sourceFile.getFileName().toString().equals("version-info.json")) {
                    Path targetFile = targetPath.resolve(sourceFile.getFileName());
                    Files.copy(sourceFile, targetFile, StandardCopyOption.REPLACE_EXISTING);
                    logger.debug("Restored {} from version {}", targetFile, versionPath);
                }
            }
        }
    }
    
    private void saveVersionInfo(Path versionPath, VersionInfo versionInfo) throws IOException {
        Path versionInfoPath = versionPath.resolve("version-info.json");
        objectMapper.writerWithDefaultPrettyPrinter()
                  .writeValue(versionInfoPath.toFile(), versionInfo);
    }
    
    @SuppressWarnings("unchecked")
    private Map<String, Object> loadVersionData(String dataName, String environment, String versionId) throws IOException {
        Path versionPath = getVersionPath(dataName, environment, versionId);
        
        // Try to load JSON file first
        Path jsonFile = versionPath.resolve(dataName + ".json");
        if (Files.exists(jsonFile)) {
            return objectMapper.readValue(jsonFile.toFile(), Map.class);
        }
        
        // Try other formats
        String[] extensions = {".yaml", ".yml", ".properties"};
        for (String ext : extensions) {
            Path dataFile = versionPath.resolve(dataName + ext);
            if (Files.exists(dataFile)) {
                if (ext.equals(".properties")) {
                    Properties props = new Properties();
                    props.load(Files.newInputStream(dataFile));
                    return new HashMap<>((Map) props);
                } else {
                    ObjectMapper yamlMapper = new ObjectMapper(new com.fasterxml.jackson.dataformat.yaml.YAMLFactory());
                    return yamlMapper.readValue(dataFile.toFile(), Map.class);
                }
            }
        }
        
        return new HashMap<>();
    }
    
    private void compareDataMaps(Map<String, Object> sourceData, Map<String, Object> targetData, VersionDiff diff) {
        Set<String> allKeys = new HashSet<>(sourceData.keySet());
        allKeys.addAll(targetData.keySet());
        
        for (String key : allKeys) {
            Object sourceValue = sourceData.get(key);
            Object targetValue = targetData.get(key);
            
            if (sourceValue == null && targetValue != null) {
                // Added field
                diff.getChanges().put(key, VersionDiff.ChangeType.ADDED);
                diff.getAddedFields().put(key, targetValue);
            } else if (sourceValue != null && targetValue == null) {
                // Removed field
                diff.getChanges().put(key, VersionDiff.ChangeType.REMOVED);
                diff.getRemovedFields().add(key);
            } else if (sourceValue != null && targetValue != null) {
                if (!Objects.equals(sourceValue, targetValue)) {
                    // Modified field
                    diff.getChanges().put(key, VersionDiff.ChangeType.MODIFIED);
                    diff.getModifiedFields().put(key, targetValue);
                } else {
                    // Unchanged field
                    diff.getChanges().put(key, VersionDiff.ChangeType.UNCHANGED);
                }
            }
        }
    }
    
    private void generateDiffSummary(VersionDiff diff) {
        int added = diff.getAddedFields().size();
        int modified = diff.getModifiedFields().size();
        int removed = diff.getRemovedFields().size();
        
        diff.setSummary(String.format("Changes: %d added, %d modified, %d removed", 
            added, modified, removed));
    }
    
    private void deleteDirectory(Path directory) throws IOException {
        if (Files.exists(directory)) {
            Files.walk(directory)
                 .sorted(Comparator.reverseOrder())
                 .forEach(path -> {
                     try {
                         Files.delete(path);
                     } catch (IOException e) {
                         logger.warn("Error deleting {}: {}", path, e.getMessage());
                     }
                 });
        }
    }
}
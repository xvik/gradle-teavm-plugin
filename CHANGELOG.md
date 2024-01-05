* Update to teavm 0.9.1
  - Removed maxTopLevelNames option
  - Add jsModuleType option
  - Add sourceFilesCopiedAsLocalLinks dev option (in teavm enum used for 3 options: no, copy, links)

Teavm behavior change: sourceFilesCopied option now works only when sourceMapsGenerated enabled 
Plugin is not compatible with teavm 0.9.0 (due to new compiler options) 

### 1.2.0 (2023-10-29)
* Update to teavm 0.9.0
    - Removed longjmpSupported option

### 1.1.0 (2023-03-08)
* Update to teavm 0.8.0
* Add new assertionsRemoved dev option (false by default)

### 1.0.1 (2023-02-16)
* Fix auto detected version usage
* Use teavm-tooling instead of teavm-cli (to workaround not published cli snapshots)

### 1.0.0 (2023-02-10)
* Initial release
# GitHub Integration Tests

Bu klasör GitHub entegrasyonu için yazılmış kapsamlı testleri içerir.

## Test Türleri

### 1. Unit Tests (Birim Testleri)
- **InstallCallbackControllerTest**: GitHub app installation callback endpoint'ini test eder
- **WebhookControllerTest**: GitHub webhook endpoint'ini test eder
- **GitHubAppAuthServiceTest**: GitHub app authentication servisini test eder
- **GitHubInstallationServiceTest**: GitHub installation servisini test eder
- **GitHubRepositoryServiceTest**: GitHub repository servisini test eder

### 2. Integration Tests (Entegrasyon Testleri)
- **GitHubIntegrationTest**: GitHub servislerinin Spring Boot context ile entegrasyonunu test eder
- **GitHubEndToEndTest**: GitHub entegrasyonunun end-to-end akışını test eder

### 3. Test Utilities (Test Yardımcıları)
- **GitHubTestUtils**: Test verileri ve yardımcı metodlar sağlar
- **GitHubTestConfig**: Test konfigürasyonu sağlar

## Testleri Çalıştırma

### Tüm GitHub Testlerini Çalıştırma
```bash
mvn test -Dtest=GitHubTestSuite
```

### Belirli Test Sınıfını Çalıştırma
```bash
# Controller testleri
mvn test -Dtest=InstallCallbackControllerTest
mvn test -Dtest=WebhookControllerTest

# Service testleri
mvn test -Dtest=GitHubAppAuthServiceTest
mvn test -Dtest=GitHubInstallationServiceTest
mvn test -Dtest=GitHubRepositoryServiceTest

# Integration testleri
mvn test -Dtest=GitHubIntegrationTest
mvn test -Dtest=GitHubEndToEndTest
```

### Test Profili ile Çalıştırma
```bash
mvn test -Dspring.profiles.active=test
```

## Test Kapsamı

### Controller Tests
- ✅ HTTP endpoint'lerin doğru mapping'i
- ✅ Request/Response handling
- ✅ HTTP status kodları
- ✅ Content type validation

### Service Tests
- ✅ Interface implementation validation
- ✅ Mock data handling
- ✅ Error scenario handling
- ✅ Input validation

### Integration Tests
- ✅ Spring Boot context loading
- ✅ Service dependency injection
- ✅ End-to-end workflow testing
- ✅ Mock service integration

## Test Verileri

Test verileri `GitHubTestUtils` sınıfında merkezi olarak yönetilir:
- Mock repository request/response objects
- Mock installation data
- Mock webhook payloads
- Mock JWT tokens

## Konfigürasyon

Test konfigürasyonu `application-test.properties` dosyasında tanımlanmıştır:
- H2 in-memory database
- Test-specific GitHub API settings
- Debug logging configuration

## Mock Services

Testlerde GitHub API çağrıları mock'lanmıştır:
- `@MockBean` annotation'ı ile Spring context'e mock servisler inject edilir
- Mockito framework'ü ile mock behavior'lar tanımlanır
- Real GitHub API çağrıları yapılmaz

## Test Sonuçları

Testler çalıştırıldığında şu bilgileri verir:
- GitHub entegrasyonunun çalışıp çalışmadığı
- Endpoint'lerin doğru çalışıp çalışmadığı
- Service'lerin beklenen davranışı sergileyip sergilemediği
- Error handling'in doğru çalışıp çalışmadığı







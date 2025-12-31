package site.protoa.api;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

@SpringBootApplication
public class ApiApplication {

	public static void main(String[] args) {
		// .env 파일 자동 로드 (프로젝트 루트에서 찾음)
		try {
			// 여러 경로에서 .env 파일 찾기
			java.util.List<Path> searchPaths = new java.util.ArrayList<>();

			// 현재 작업 디렉토리와 상위 디렉토리들
			Path currentDir = Paths.get("").toAbsolutePath();
			Path userDir = Paths.get(System.getProperty("user.dir"));

			// 최대 5단계까지 상위 디렉토리 탐색
			for (int i = 0; i < 5; i++) {
				searchPaths.add(currentDir);
				searchPaths.add(userDir);
				if (currentDir.getParent() != null) {
					currentDir = currentDir.getParent();
				}
				if (userDir.getParent() != null) {
					userDir = userDir.getParent();
				}
			}

			// 클래스 파일 위치 기반 탐색 (안전하게)
			try {
				Path classPath = Paths.get(ApiApplication.class.getProtectionDomain()
						.getCodeSource().getLocation().toURI());
				for (int i = 0; i < 5 && classPath != null; i++) {
					searchPaths.add(classPath);
					classPath = classPath.getParent();
				}
			} catch (Exception e) {
				// 클래스 경로를 찾을 수 없으면 무시
			}

			File envFile = null;
			Path rootPath = null;

			// .env 파일 찾기
			for (Path path : searchPaths) {
				if (path == null)
					continue;
				try {
					File testFile = path.resolve(".env").toFile();
					if (testFile.exists() && testFile.isFile()) {
						envFile = testFile;
						rootPath = path;
						System.out.println("✓ .env 파일 발견: " + testFile.getAbsolutePath());
						break;
					}
				} catch (Exception e) {
					// 경로 접근 실패 시 다음 경로 시도
					continue;
				}
			}

			if (envFile != null && rootPath != null) {
				try {
					Dotenv dotenv = Dotenv.configure()
							.directory(rootPath.toString())
							.ignoreIfMissing()
							.load();

					// 환경 변수를 시스템 속성으로 설정 (Spring Boot가 읽을 수 있도록)
					int loadedCount = 0;
					// dotenv.entries()는 Iterable<DotenvEntry>를 반환
					for (io.github.cdimascio.dotenv.DotenvEntry entry : dotenv.entries()) {
						String key = entry.getKey();
						String value = entry.getValue();
						// 기존 시스템 속성이 없을 때만 설정 (환경 변수 우선순위)
						if (System.getProperty(key) == null && System.getenv(key) == null) {
							System.setProperty(key, value);
							loadedCount++;
						}
					}

					System.out.println("✓ .env 파일 자동 로드 완료 (" + loadedCount + "개 환경 변수 로드됨)");
				} catch (Exception dotenvException) {
					// 중복 키 등의 오류 발생 시, 직접 파일을 읽어서 처리
					System.out.println("⚠️ 경고: dotenv 라이브러리 로드 실패 (중복 키 가능성): " + dotenvException.getMessage());
					System.out.println("   .env 파일을 직접 읽어서 처리합니다...");

					try {
						java.util.List<String> lines = java.nio.file.Files.readAllLines(envFile.toPath());
						java.util.Set<String> loadedKeys = new java.util.HashSet<>();
						int loadedCount = 0;

						for (String line : lines) {
							line = line.trim();
							// 주석과 빈 줄 건너뛰기
							if (line.isEmpty() || line.startsWith("#")) {
								continue;
							}

							// KEY=VALUE 형식 파싱
							int equalsIndex = line.indexOf('=');
							if (equalsIndex > 0) {
								String key = line.substring(0, equalsIndex).trim();
								String value = line.substring(equalsIndex + 1).trim();

								// 따옴표 제거
								if ((value.startsWith("\"") && value.endsWith("\"")) ||
										(value.startsWith("'") && value.endsWith("'"))) {
									value = value.substring(1, value.length() - 1);
								}

								// 중복 키는 첫 번째 값만 사용
								if (!loadedKeys.contains(key) &&
										System.getProperty(key) == null &&
										System.getenv(key) == null) {
									System.setProperty(key, value);
									loadedKeys.add(key);
									loadedCount++;

									// GOOGLE_CLIENT_ID 로드 확인
									if (key.equals("GOOGLE_CLIENT_ID")) {
										System.out.println("   ✓ GOOGLE_CLIENT_ID 로드됨: " +
												(value.length() > 20 ? value.substring(0, 20) + "..." : value));
									}
								}
							}
						}

						System.out.println("✓ .env 파일 직접 로드 완료 (" + loadedCount + "개 환경 변수 로드됨)");
					} catch (Exception fileException) {
						System.out.println("⚠️ 경고: .env 파일 직접 읽기 실패: " + fileException.getMessage());
					}
				}
			} else {
				System.out.println("⚠️ 경고: .env 파일을 찾을 수 없습니다. 시스템 환경 변수를 사용합니다.");
				System.out.println("   현재 작업 디렉토리: " + Paths.get("").toAbsolutePath());
				System.out.println("   시스템 작업 디렉토리: " + System.getProperty("user.dir"));
			}
		} catch (Exception e) {
			// .env 파일이 없거나 로드 실패해도 계속 진행
			System.out.println("⚠️ 경고: .env 파일 로드 실패: " + e.getMessage());
			System.out.println("   시스템 환경 변수를 사용합니다.");
		}

		SpringApplication.run(ApiApplication.class, args);
	}

}

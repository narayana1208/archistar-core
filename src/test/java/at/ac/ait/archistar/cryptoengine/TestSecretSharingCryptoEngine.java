package at.ac.ait.archistar.cryptoengine;

import static org.fest.assertions.api.Assertions.*;

import java.util.HashSet;
import java.util.Set;

import org.junit.BeforeClass;
import org.junit.Test;

import at.ac.ait.archistar.backendserver.fragments.Fragment;
import at.ac.ait.archistar.backendserver.fragments.RemoteFragment;
import at.archistar.crypto.SecretSharing;
import at.archistar.crypto.ShamirPSS;
import at.archistar.crypto.random.FakeRandomSource;
import at.ac.ait.archistar.middleware.crypto.CryptoEngine;
import at.ac.ait.archistar.middleware.crypto.DecryptionException;
import at.ac.ait.archistar.middleware.crypto.SecretSharingCryptoEngine;

public class TestSecretSharingCryptoEngine {
	
	private static CryptoEngine cryptoEngine;
	private final static byte[] mockSerializedData = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
	
	@BeforeClass
	public static void onceSetup() {
		
		SecretSharing alg = new ShamirPSS(5, 3, new FakeRandomSource());
		
		// GIVEN some test data
		cryptoEngine = new SecretSharingCryptoEngine(alg);
	}
	
	@Test
	public void testIfDecryptionProducesOriginalData() {
		
		Set<Fragment> distribution = new HashSet<Fragment>();
		distribution.add(new RemoteFragment("frag-1"));
		distribution.add(new RemoteFragment("frag-2"));
		distribution.add(new RemoteFragment("frag-3"));
		distribution.add(new RemoteFragment("frag-4"));
		
		Set<Fragment> encrypted = cryptoEngine.encrypt(mockSerializedData, distribution);
		
		assertThat(encrypted.size()).isEqualTo(4);
		
		for(Fragment f : encrypted) {
			assertThat(f.getData()).isNotNull();
			assertThat(f.getData()).isNotEmpty();
		}
		
		byte[] result = null;
		try {
			result = cryptoEngine.decrypt(encrypted);
		} catch (DecryptionException e) {
			fail("error while decryption", e);
		}
		assertThat(result).isNotNull().isEqualTo(mockSerializedData);
	}

}

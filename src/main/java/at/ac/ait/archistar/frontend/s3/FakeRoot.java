package at.ac.ait.archistar.frontend.s3;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.xml.parsers.ParserConfigurationException;

import org.jboss.resteasy.util.Hex;

import at.ac.ait.archistar.middleware.Engine;
import at.ac.ait.archistar.middleware.crypto.DecryptionException;
import at.ac.ait.archistar.middleware.frontend.FSObject;
import at.ac.ait.archistar.middleware.frontend.SimpleFile;

@Path("/")
public class FakeRoot {
	
	private Engine engine;
	
	private XmlDocumentBuilder builder;

	public FakeRoot(Engine engine) throws ParserConfigurationException {
		this.engine = engine;
		this.builder = new XmlDocumentBuilder();
	}
		
	public String listBuckets() {
		return builder.stringFromDoc(builder.listBuckets());
	}
	
	/* need to do this in a cleaner way */
	@GET
	@Produces("text/plain")
	public String getAll(
			@QueryParam("delimiter") String delim,
            @QueryParam("prefix") String prefix,
            @QueryParam("max-keys") int maxKeysInt) throws DecryptionException {
		
		if (prefix != null && (prefix.equals("/") || prefix.equals(""))) {
			prefix = null;
		}
		
		if (prefix != null && prefix.startsWith("/fake_bucket")) {
			prefix = prefix.substring(12);
		}
		
		if (prefix != null && (prefix.equals("/") || prefix.equals(""))) {
			prefix = null;
		}
		
		System.err.println("prefix: " + prefix);

		HashSet<SimpleFile> results = new HashSet<SimpleFile>();
		for(String key : this.engine.listObjects(prefix)) {
			FSObject obj = engine.getObject(key);
			
			if (obj instanceof SimpleFile) {
				results.add((SimpleFile) obj);
			}
		}
		
		return builder.stringFromDoc(builder.listElements(prefix, maxKeysInt, results));
	}
	
	@GET
	@Path( "{id:.+}")
	@Produces ("text/plain")
	public Response getById(@PathParam("id") String id
			) throws DecryptionException, NoSuchAlgorithmException {
		
		System.err.println("get by id:" + id);
		
		FSObject obj = engine.getObject(id);
		byte[] result = null;
		
		if (obj instanceof SimpleFile) {
			result = ((SimpleFile) obj).getData();
		}
		
		MessageDigest md = MessageDigest.getInstance("MD5");
		byte[] thedigest = md.digest(result);
		
		return Response.accepted().entity(result).header("ETag", new String(Hex.encodeHex(thedigest))).build();
	}

	@HEAD
	@Path( "{id:.+}")
	@Produces ("text/plain")
	public Response getStatById(@PathParam("id") String id) throws DecryptionException, NoSuchAlgorithmException {
		
		Map<String, String> result = engine.statObject(id);
		
		if (result != null) {
			FSObject obj = engine.getObject(id);
			
			SimpleFile file = null;
			if (obj instanceof SimpleFile) {
				file = (SimpleFile) obj;
			}
			
			MessageDigest md = MessageDigest.getInstance("MD5");
			byte[] thedigest = md.digest(file.getData());
			String etag = new String(Hex.encodeHex(thedigest));
			
			ResponseBuilder resp = Response.accepted().status(200);
			
			System.err.println("Content-Length is " + file.getData().length);
			
			resp.header("Content-Length", "" + file.getData().length);
			resp.header("x-foobar", "" + file.getData().length);
			resp.header("ETag", etag);
			
			Map<String, String> md1 = file.getMetadata();
			
			for(String i : md1.keySet()) {
				System.err.println("metadata: " + i + " -> " + md1.get(i));
			}
			
			if(md1.get("uid") != null) {
				resp.header("x-amz-meta-uid", md1.get("uid").replace("\r\n",""));
			}
			
			if(md1.get("gid") != null) {
				resp.header("x-amz-meta-gid", md1.get("gid").replace("\r\n",""));
			}
			
			
			if(md1.get("mode") != null) {
				resp.header("x-amz-meta-mode", md1.get("mode").replace("\r\n",""));
			}
			
			Response r = resp.build();
			
			System.err.println("r->length: " + r.getLength());
			System.err.println("returning 200");
			return r;
		} else {
			System.err.println("returning 404");
			return Response.accepted().status(404).build();
		}		
	}
	
	@PUT
	@Path( "{id:.+}")
	@Produces ("text/xml")
	public Response writeById(@PathParam("id") String id,
						   @HeaderParam("x-amz-server-side-encryption") String serverSideEncryption,
						   @HeaderParam("x-amz-meta-gid") String gid,
						   @HeaderParam("x-amz-meta-uid") String uid,
						   @HeaderParam("x-amz-meta-mode") String mode,
						   byte[] input) throws NoSuchAlgorithmException, DecryptionException {
		
		System.err.println("PUT by id: " + id);
		
		SimpleFile obj = new SimpleFile(id, input, new HashMap<String, String>());
		
		obj.setMetaData("gid", gid);
		obj.setMetaData("uid", uid);
		obj.setMetaData("mode", mode);
		
		engine.putObject(obj);
		
		MessageDigest md = MessageDigest.getInstance("MD5");
		byte[] thedigest = md.digest(input);
		
		engine.getObject(id);
		
		return Response.accepted().status(200).header("ETag", new String(Hex.encodeHex(thedigest))).build();
	}

	@DELETE
	@Path( "{id:.+}")
	@Produces ("text/plain")
	public Response deleteById(@PathParam("id") String id) throws DecryptionException {
		FSObject obj = engine.getObject(id);
		engine.deleteObject(obj);
		return Response.accepted().status(204).build();
	}	
}

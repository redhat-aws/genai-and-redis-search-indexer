package com.enterprisewebservice.api;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.GET;
import java.util.List;
import java.util.ArrayList;
import com.enterprisewebservice.api.NoteService;
import com.enterprisewebservice.model.StrapiServiceResponse;
import com.enterprisewebservice.embeddings.EmbeddingService;
import com.enterprisewebservice.model.Note;
import com.enterprisewebservice.model.StrapiEventPayload;
import com.enterprisewebservice.embeddings.EmbeddingResponse;
import com.enterprisewebservice.embeddings.ChunkingService;

import com.enterprisewebservice.RedisSearchIndexer;

import io.micrometer.core.annotation.Counted;
import io.micrometer.core.instrument.MeterRegistry;

@Path("/assessments")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class GPTAssessmentResource {

    @Inject
    NoteService noteService;

    @Inject
    RedisSearchIndexer redisSearchIndexer;
    
    @Inject
    EmbeddingService embeddingService;

    @Inject
    MeterRegistry registry;


    @GET
    @Path("/test")
    public Response getTestValue() {
        return Response.ok("Test value").build();
    }


    @POST
    @Counted(value = "note_create_embedding")
    @Path("/create-embedding")
    public Response getNoteAndIndex(StrapiEventPayload payload) {
        try {
           if (payload == null || !payload.getModel().equals("note")) {
                return Response.status(Response.Status.BAD_REQUEST).entity("Payload is not a note").build();
            }
           System.out.println("Payload: " + payload.toString());
            Note note = (Note)payload.getEntry();
            Long id = note.getId();

            StrapiServiceResponse<Note> result = noteService.getNote(id);

            // Assuming the object has a method for getting the data
            note = result.getData();

            String keycloakSubject = result.getData().getUserProfile().getKeycloaksubject();

            // Create the index
            redisSearchIndexer.createIndex();
            List<String> texts = ChunkingService.chunkObject(note.getName() + " " + note.getRichText());
            EmbeddingResponse embeddingResponse = embeddingService.generateEmbeddings(texts);
            

            // Index the embeddings
            redisSearchIndexer.indexEmbeddings(embeddingResponse, note.getId().toString(), texts, keycloakSubject);

            return Response.ok(note).build(); // returns the note object

        } catch (Exception e) {
            // Handle exception appropriately
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        }
    }

    @POST
    @Path("/delete-embedding")
    public Response deleteNoteIndex(StrapiEventPayload payload) {
        try {
           if (payload == null || !payload.getModel().equals("note")) {
                return Response.status(Response.Status.BAD_REQUEST).entity("Payload is not a note").build();
            }
            System.out.println("Payload: " + payload.toString());
            Note note = (Note)payload.getEntry();
            Long id = note.getId();

            StrapiServiceResponse<Note> result = noteService.getNote(id);

            // Assuming the object has a method for getting the data

            String keycloakSubject = result.getData().getUserProfile().getKeycloaksubject();
            // Assuming the object has a method for getting the data
           
            // Index the embeddings
            redisSearchIndexer.deleteEmbedding(id.toString());

            return Response.ok(id).build(); // returns the note object

        } catch (Exception e) {
            // Handle exception appropriately
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        }
    }

    @DELETE
    @Path("/delete-all-embeddings")
    public Response deleteEmbeddings() {
        try {
           
            redisSearchIndexer.deleteDocuments();
            return Response.ok().build(); // returns the note object

        } catch (Exception e) {
            // Handle exception appropriately
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        }
    }
}

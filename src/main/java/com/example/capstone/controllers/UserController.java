package com.example.capstone.controllers;


import com.example.capstone.exception.EmailDuplicateException;
import com.example.capstone.exception.UserNotFound;
import com.example.capstone.exception.UsernameDuplicateException;
import com.example.capstone.model.User;
import com.example.capstone.payload.UserDTO;
import com.example.capstone.payload.request.LoginRequest;
import com.example.capstone.payload.request.RegistrationRequest;
import com.example.capstone.payload.response.ErrorResponse;
import com.example.capstone.payload.response.LoginResponse;
import com.example.capstone.security.jwt.JwtUtils;
import com.example.capstone.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/user")
@CrossOrigin (origins = "http://localhost:5174")
public class UserController {


    @Autowired
    UserService userService;

    @Autowired
    AuthenticationManager authenticationManager;

    @Autowired
    JwtUtils jwtUtils;

    @PostMapping(value = "/new")
    public ResponseEntity<?> registerUser(@RequestBody @Validated RegistrationRequest nuovoUtente, BindingResult validazione) throws InterruptedException{
     try {
         if (validazione.hasErrors()) {
             StringBuilder errors = new StringBuilder("Problemi nella validazione dati: \n");

             for (ObjectError err : validazione.getAllErrors()) {
                 errors.append(err.getDefaultMessage()).append("\n");
             }
             return new ResponseEntity<>(errors.toString(), HttpStatus.BAD_REQUEST);
         }

         User user = userService.registerNewUser(nuovoUtente);
         return ResponseEntity.status(HttpStatus.CREATED).body(user);
     }catch (EmailDuplicateException e){
         return new ResponseEntity<>("EMAIL GIA REGISTRATA",HttpStatus.BAD_REQUEST);
     }catch (UsernameDuplicateException e){
         return new ResponseEntity<>("USERNAME GIA IN USO",HttpStatus.BAD_REQUEST);
     }
    }

    @PostMapping("/auth/login")
    public ResponseEntity<?> login(@Validated @RequestBody LoginRequest loginRequest, BindingResult validazione){
        if(validazione.hasErrors()){
            StringBuilder errors= new StringBuilder("Problemi nella validazione dati: \n");
            for(ObjectError err:validazione.getAllErrors()){
                errors.append(err.getDefaultMessage()).append("\n");
            }

            return new ResponseEntity<>(errors.toString(),HttpStatus.BAD_REQUEST);
        }

        try{
            String username=loginRequest.getUsername();
            System.out.println(username);
            Authentication authentication=
                    authenticationManager.authenticate(
                            new UsernamePasswordAuthenticationToken(username,loginRequest.getPassword()));
            System.out.println(authentication);

            String token=jwtUtils.createJwtToken(authentication);
            System.out.println("🔹 Token generato: " + token);
            LoginResponse loginResponse=new LoginResponse(username,token);
            return ResponseEntity.ok(loginResponse);
        }catch (BadCredentialsException e){
            ErrorResponse errorResponse=new ErrorResponse(HttpStatus.BAD_REQUEST,"INVALID USERNAME OR PASSWORD");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        } catch (Exception e){
            ErrorResponse errorResponse=new ErrorResponse(HttpStatus.BAD_REQUEST, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }

    }

    /*@PostMapping("/newAdmin")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<User> registerAdmin(@RequestBody UserDTO userDTO) throws InterruptedException{
        User user= userService.registerNewUser(userDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(user);
    }*/

    @PatchMapping("{id}/image")
    @PreAuthorize("hasAuthority('USER') or hasAuthority('ADMIN')")
    public ResponseEntity<?> uploadImafw(@RequestPart ("avatar")MultipartFile file,@PathVariable long id){

        try {
            String message= userService.uploadImage(id,file);
            return ResponseEntity.status(HttpStatus.CREATED).body(message);

        } catch (UserNotFound e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Errore nel caricamento dell'immagine: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Errore generico: " + e.getMessage());
        }
    }


    @PutMapping("{id}")
    @PreAuthorize("hasAuthority('USER') or hasAuthority('ADMIN')")
    public ResponseEntity<?> modifyUser(@PathVariable long id, @RequestBody UserDTO userDTO){

        User user=userService.editUser(userDTO, id);
        return  ResponseEntity.status(HttpStatus.OK).body("Utente " + user.getUsername() +" modificato");

    }


    @DeleteMapping("{id}")
    @PreAuthorize("hasAuthority('USER') or hasAuthority('ADMIN')")
    public ResponseEntity<?> deleteUser(@PathVariable long id){

        try {
            String message=userService.deleteUser(id);
            return ResponseEntity.status(HttpStatus.OK).body(message);
        } catch (UserNotFound e){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Errore generico: " + e.getMessage());
        }
    }
}

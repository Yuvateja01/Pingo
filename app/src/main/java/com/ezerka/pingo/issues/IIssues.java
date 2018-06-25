package com.ezerka.pingo.issues;

import com.ezerka.pingo.model.Issue;
import com.ezerka.pingo.model.Project;

import java.util.ArrayList;


/**
 * Created by User on 4/16/2018.
 */

public interface IIssues {

  void showProgressBar();

  void hideProgressBar();

  void buildSnackbar(String message);

  void getProjects();

  void deleteIssuesFromProject(ArrayList<Issue> issues, Project project);
}

# IVT Baseline Scenario

[![Build Status](https://travis-ci.org/matsim-eth/av.svg?branch=master)](https://travis-ci.org/matsim-eth/baseline_scenario)

This repository contains the code for the Switzerland/Zurich scenarios for MATSim of the Institute 
for Transport Planning and Systems at ETH Zurich.

It consists of several components:
- The whole scenario synthesis process which creates the MATSim population etc. from the raw data sources available at the institute.
- A highly advanced scenario cutter (for Zurich or any other case), which is able to extract a smaller scenario from a bigger one
while maintaining realistic entry points into the scenario for the background traffic with all modes, including cars and public transport.
- A fast public transport simulation engine, which moves agents according to the public transit schedule. Only interchanges at stops are
considered, otherwise no further network events are thrown to speed up the simulation.
- Several tools for routing, modifying and adjusting populations.

This project is an extension of the MATSim transport simulation framework:
https://github.com/matsim-org/matsim

Maintenance: Sebastian Hörl

## Working with the repository

The latest changes are in the `master` branch. The version there is always a SNAPSHOT version. If you want to use it, clone the git repository, or use the [packagecloud](https://packagecloud.io/eth-ivt/matsim/packages/java/ch.ethz.matsim/av-0.1.2-SNAPSHOT.jar) repository for Maven packages. Release versions are not offered at the moment as the project is still rather volatile.
